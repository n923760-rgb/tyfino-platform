#!/usr/bin/env bash

set -Eeuo pipefail

readonly TYFINO_BOOTSTRAP_VERSION="1"
readonly DEPLOY_USER="tyfino"
readonly APP_ROOT="/opt/tyfino"
readonly SSH_HARDENING_FILE="/etc/ssh/sshd_config.d/60-tyfino-baseline.conf"
readonly DOCKER_DAEMON_FILE="/etc/docker/daemon.json"
readonly DOCKER_LOGROTATE_FILE="/etc/logrotate.d/tyfino-docker"
readonly SUDOERS_FILE="/etc/sudoers.d/90-tyfino"

log() {
  printf '[tyfino-bootstrap] %s\n' "$*"
}

fail() {
  printf '[tyfino-bootstrap] ERROR: %s\n' "$*" >&2
  exit 1
}

if [[ "${EUID}" -ne 0 ]]; then
  fail "Run this script as root."
fi

if [[ ! -r /etc/os-release ]]; then
  fail "Cannot identify the operating system."
fi

# shellcheck disable=SC1091
source /etc/os-release
if [[ "${ID:-}" != "ubuntu" ]]; then
  fail "This bootstrap supports Ubuntu only."
fi

export DEBIAN_FRONTEND=noninteractive

log "Starting server baseline v${TYFINO_BOOTSTRAP_VERSION} on Ubuntu ${VERSION_ID:-unknown}."

apt-get update
apt-get install -y \
  ca-certificates \
  curl \
  fail2ban \
  git \
  gnupg \
  jq \
  rsync \
  unattended-upgrades \
  ufw

if ! id "${DEPLOY_USER}" >/dev/null 2>&1; then
  adduser --disabled-password --gecos "" "${DEPLOY_USER}"
fi
usermod -aG sudo "${DEPLOY_USER}"

printf '%s ALL=(ALL:ALL) NOPASSWD: ALL\n' "${DEPLOY_USER}" > "${SUDOERS_FILE}"
chmod 440 "${SUDOERS_FILE}"
visudo -cf "${SUDOERS_FILE}"

install -d -m 700 -o "${DEPLOY_USER}" -g "${DEPLOY_USER}" "/home/${DEPLOY_USER}/.ssh"
if [[ -s /root/.ssh/authorized_keys ]]; then
  install -m 600 -o "${DEPLOY_USER}" -g "${DEPLOY_USER}" \
    /root/.ssh/authorized_keys "/home/${DEPLOY_USER}/.ssh/authorized_keys"
else
  fail "/root/.ssh/authorized_keys is missing or empty; refusing to create an inaccessible deploy user."
fi

install -d -m 755 /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
  | gpg --dearmor --yes -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg

dpkg_arch="$(dpkg --print-architecture)"
ubuntu_codename="${VERSION_CODENAME:-}"
if [[ -z "${ubuntu_codename}" ]]; then
  fail "Ubuntu version codename is unavailable."
fi
printf 'deb [arch=%s signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu %s stable\n' \
  "${dpkg_arch}" "${ubuntu_codename}" > /etc/apt/sources.list.d/docker.list

apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
usermod -aG docker "${DEPLOY_USER}"

install -d -m 755 /etc/docker
cat > "${DOCKER_DAEMON_FILE}" <<'EOF'
{
  "live-restore": true,
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "5"
  }
}
EOF

cat > "${DOCKER_LOGROTATE_FILE}" <<'EOF'
/var/lib/docker/containers/*/*.log {
  rotate 5
  daily
  compress
  size 10M
  missingok
  delaycompress
  copytruncate
}
EOF

systemctl enable --now docker

if ! swapon --show=NAME --noheadings | grep -q .; then
  if [[ ! -e /swapfile ]]; then
    fallocate -l 2G /swapfile || dd if=/dev/zero of=/swapfile bs=1M count=2048 status=progress
  fi
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
fi
if ! grep -Eq '^/swapfile[[:space:]]' /etc/fstab; then
  printf '/swapfile none swap sw 0 0\n' >> /etc/fstab
fi

cat > /etc/fail2ban/jail.d/sshd.local <<'EOF'
[sshd]
enabled = true
maxretry = 5
findtime = 10m
bantime = 1h
EOF
systemctl enable --now fail2ban

cat > "${SSH_HARDENING_FILE}" <<'EOF'
X11Forwarding no
PermitEmptyPasswords no
MaxAuthTries 5
LoginGraceTime 30
EOF
sshd -t
systemctl reload ssh

ufw default deny incoming
ufw default allow outgoing
ufw allow OpenSSH
ufw allow 80/tcp
ufw allow 443/tcp
ufw allow 443/udp
ufw --force enable

cat > /etc/apt/apt.conf.d/20auto-upgrades <<'EOF'
APT::Periodic::Update-Package-Lists "1";
APT::Periodic::Unattended-Upgrade "1";
EOF
systemctl enable --now unattended-upgrades

install -d -m 750 -o "${DEPLOY_USER}" -g "${DEPLOY_USER}" "${APP_ROOT}"

docker version >/dev/null
docker compose version >/dev/null
ufw status | grep -q 'Status: active'
fail2ban-client status sshd >/dev/null
swapon --show=NAME --noheadings | grep -q .

log "Baseline complete."
log "Deploy user: ${DEPLOY_USER}"
log "Application directory: ${APP_ROOT}"
log "Root/password SSH policy was intentionally left unchanged pending a verified key-login test."
