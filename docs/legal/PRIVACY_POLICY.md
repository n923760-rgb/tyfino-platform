# TYFINO Privacy Policy

Status: ENGINEERING DRAFT — NOT YET A PUBLISHED LEGAL POLICY  
Last reviewed: 2026-09-27  
Languages: English / العربية  
Scope: TYFINO Android application, application licensing service, and administration metadata

> This document is an engineering draft for owner and legal review. It must be reviewed, completed with the legal entity/contact details, and published at a stable public URL before production distribution.

---

# English

## 1. Who this policy applies to

This Privacy Policy describes how TYFINO handles personal data in connection with the TYFINO Android application, application-licensing service, and authorized administration functions.

Controller / legal entity: **[OWNER TO COMPLETE]**  
Privacy contact: **[OWNER TO COMPLETE]**  
Business address: **[OWNER TO COMPLETE IF REQUIRED]**

TYFINO is an application player and licensing platform. TYFINO does not provide or manage IPTV subscriptions.

## 2. Data TYFINO may process

### Application licensing data

The licensing service may process data necessary to operate application licensing, such as:

- opaque random Installation ID;
- app platform/version;
- trial start/status;
- license type and status;
- activation and entitlement timestamps;
- last successful entitlement verification;
- licensing session state;
- revocation/reset state;
- request IDs and bounded operational/security metadata.

The Android Installation ID is an application-generated opaque identifier. It is not intended to be a MAC address, IMEI, Android ID, Advertising ID, or hardware fingerprint.

### Administrative-only metadata

Authorized administrators may optionally store support/business metadata such as:

- Customer Name;
- Phone Number;
- External Reference;
- Admin Label;
- Internal Note.

This information is administrative-only and must not be returned to the Android application unless a future approved requirement explicitly allows it.

### Security and audit data

TYFINO may process redacted security/audit information necessary to protect the service and record authorized administrative actions, such as:

- administrator identifier;
- action type;
- timestamp;
- redacted target/reference;
- request ID;
- security event outcome.

## 3. Data TYFINO does not collect through its licensing service

TYFINO's licensing backend is designed not to receive or store:

- IPTV Host;
- IPTV Username;
- IPTV Password;
- M3U URLs or content;
- Stalker/MAC Portal data;
- IPTV package/subscription details;
- channel/movie/series catalogs;
- EPG data;
- IPTV stream URLs;
- IPTV viewing history;
- device MAC address as an activation credential;
- IMEI;
- Advertising ID;
- precise or approximate location for licensing.

The Android application connects directly to the user's IPTV provider for provider authentication, catalog, EPG, and playback.

## 4. Purposes of processing

TYFINO may process personal data only for defined purposes, including:

- starting and administering the explicit seven-day application trial;
- activating and verifying application licenses;
- enforcing the approved one-active-installation rule;
- revoking/resetting licensing sessions when authorized;
- providing customer/support administration;
- preventing abuse and protecting service security;
- maintaining redacted administrative audit records;
- diagnosing bounded service failures.

Personal data must not be repurposed in a way inconsistent with the approved TYFINO product/data-ownership boundary.

## 5. Legal basis

The applicable legal basis depends on the processing activity and final commercial/legal setup.

Before production publication, the owner/legal reviewer must map each processing activity to its applicable lawful basis under Saudi law and any other applicable jurisdiction.

TYFINO must not claim consent, contract, legal obligation, or legitimate interest without confirming that basis for the specific activity.

## 6. Storage and security

TYFINO uses technical and organizational controls appropriate to the data and service boundary.

Repository-approved controls include:

- separation between IPTV provider credentials and TYFINO licensing;
- HTTPS for production TYFINO APIs;
- secret redaction from ordinary logs;
- HMAC-based storage of Activation Codes on the backend rather than plaintext persistence;
- restricted runtime database role;
- audited administrative actions;
- Android Keystore-backed protection for approved local secret material;
- exclusion of protected Android credential/licensing storage from backup where required by the Android contracts.

Production operational controls, backup location, retention, monitoring, and signing-key custody remain separately qualified release gates.

## 7. Sharing and disclosure

TYFINO should disclose personal data only where needed for approved service operation, service providers/processors, legal compliance, security, or other lawful purposes.

Before production, the public policy must identify relevant categories of recipients/processors and whether personal data is transferred or processed outside Saudi Arabia.

No statement in this draft authorizes a new third-party analytics, advertising, crash-reporting, or tracking SDK.

## 8. Retention

Retention periods are **not yet fully approved**.

TYFINO will retain personal data only for the period justified by the applicable purpose, legal obligations, security/audit requirements, and approved retention schedule.

The recommendations document proposed fixed periods for some data, but those periods are not adopted by this draft without legal and operational review.

## 9. Your rights

Subject to applicable law and exceptions, data subjects may have rights including:

- to be informed about processing;
- to access personal data;
- to obtain a readable copy;
- to request correction/completion/update;
- to request destruction/deletion when applicable;
- to withdraw consent where processing is based on consent.

TYFINO must provide a practical mechanism for exercising applicable rights before production launch.

See `docs/legal/DATA_DELETION.md`.

## 10. International transfers

Whether TYFINO personal data will be transferred, disclosed, stored, or processed outside Saudi Arabia is **OPEN** until the production hosting/provider architecture is approved.

The final public policy must accurately disclose the approved arrangement and applicable safeguards.

## 11. Children

TYFINO is not currently designed as a children-specific service.

Any future targeting of children or processing that creates a children-specific compliance obligation requires separate review before release.

## 12. Changes to this policy

Material privacy changes must be reviewed against the actual release behavior and communicated/published as required.

The policy version shown to users must match the data practices of the distributed application.

## 13. Contact

Privacy inquiries and data-rights requests:

**[OWNER TO PROVIDE PRIVACY CONTACT / PUBLIC REQUEST CHANNEL]**

---

# العربية

## 1. نطاق السياسة

توضح هذه السياسة كيفية تعامل TYFINO مع البيانات الشخصية المرتبطة بتطبيق Android وخدمة ترخيص التطبيق ووظائف الإدارة المصرح بها.

جهة التحكم / الكيان القانوني: **[يُستكمل من المالك]**  
وسيلة التواصل للخصوصية: **[يُستكمل من المالك]**  
العنوان النظامي عند الحاجة: **[يُستكمل من المالك]**

TYFINO هو مشغل وسيلة وخدمة لترخيص التطبيق، ولا يوفّر أو يدير اشتراكات IPTV.

## 2. البيانات التي قد تعالجها TYFINO

### بيانات ترخيص التطبيق

قد تعالج خدمة الترخيص البيانات اللازمة لتشغيل الترخيص، مثل:

- معرّف تثبيت عشوائي وغير مباشر;
- المنصة وإصدار التطبيق;
- حالة وبداية التجربة;
- نوع الترخيص وحالته;
- أوقات التفعيل والتحقق;
- حالة جلسة الترخيص;
- حالة الإلغاء أو إعادة ربط الجهاز;
- معرّفات الطلبات وبيانات تشغيل/أمان محدودة.

معرّف التثبيت ليس مصممًا ليكون MAC Address أو IMEI أو Android ID أو Advertising ID أو بصمة عتاد.

### بيانات إدارية اختيارية

قد يضيف المسؤول المصرح له بيانات دعم/إدارة مثل:

- اسم العميل;
- رقم الجوال;
- مرجع خارجي;
- تسمية إدارية;
- ملاحظة داخلية.

هذه البيانات إدارية فقط ولا تُعاد إلى تطبيق Android إلا إذا اعتمد مستقبلًا متطلب صريح يسمح بذلك.

### بيانات الأمان والتدقيق

قد تتم معالجة بيانات تدقيق وأمان محدودة مثل:

- هوية المسؤول;
- نوع الإجراء;
- الوقت;
- مرجع هدف مخفي/مختصر;
- معرّف الطلب;
- نتيجة الحدث الأمني.

## 3. البيانات التي لا تجمعها خدمة ترخيص TYFINO

خدمة الترخيص مصممة بحيث لا تستقبل أو تخزن:

- Host الخاص بمزود IPTV;
- Username الخاص بـ IPTV;
- Password الخاص بـ IPTV;
- روابط أو محتوى M3U;
- بيانات Stalker/MAC Portal;
- تفاصيل باقات أو اشتراكات مزود IPTV;
- قوائم القنوات أو الأفلام أو المسلسلات;
- EPG;
- روابط البث;
- سجل مشاهدة IPTV;
- MAC Address كوسيلة تفعيل;
- IMEI;
- Advertising ID;
- الموقع الدقيق أو التقريبي لأغراض الترخيص.

يتصل تطبيق Android مباشرة بمزود IPTV للمصادقة وجلب المحتوى وEPG والتشغيل.

## 4. أغراض المعالجة

قد تستخدم TYFINO البيانات فقط لأغراض محددة مثل:

- بدء وإدارة تجربة التطبيق المجانية لمدة سبعة أيام بعد طلب المستخدم;
- تفعيل تراخيص التطبيق والتحقق منها;
- تطبيق قاعدة تثبيت فعال واحد;
- إلغاء/إعادة ضبط جلسات الترخيص عند التصريح;
- الدعم والإدارة;
- منع إساءة الاستخدام وحماية الخدمة;
- حفظ سجلات تدقيق إدارية منقحة;
- تشخيص أعطال الخدمة بشكل محدود.

## 5. المسوغ النظامي

يختلف المسوغ النظامي بحسب نوع المعالجة والترتيب التجاري والقانوني النهائي.

قبل الإطلاق الإنتاجي يجب على المالك/المراجع القانوني ربط كل عملية معالجة بمسوغها النظامي المناسب وفق نظام حماية البيانات الشخصية وأي أنظمة أخرى منطبقة.

## 6. التخزين والأمان

تعتمد TYFINO ضوابط تقنية وتنظيمية تتناسب مع حدود المنتج، ومنها:

- الفصل بين بيانات مزود IPTV وخدمة ترخيص TYFINO;
- HTTPS لخدمات TYFINO الإنتاجية;
- حجب الأسرار من السجلات العادية;
- عدم تخزين رموز التفعيل كاملة كنص صريح في قاعدة البيانات;
- صلاحيات مقيدة لحساب قاعدة بيانات التطبيق;
- تدقيق الإجراءات الإدارية;
- حماية الأسرار المحلية على Android باستخدام Android Keystore وفق العقود المعتمدة.

تبقى الاستضافة الإنتاجية والنسخ الاحتياطي والاحتفاظ والرصد وحفظ مفاتيح التوقيع بوابات تأهيل مستقلة.

## 7. المشاركة والإفصاح

لا يتم الإفصاح عن البيانات الشخصية إلا بقدر ما يلزم لتشغيل الخدمة المعتمد أو لمزودي الخدمة/المعالجة أو للالتزام النظامي أو الأمان أو غير ذلك من الأغراض المشروعة.

قبل الإنتاج يجب أن تحدد السياسة المنشورة فئات الجهات المستلمة/المعالجة، وهل ستُنقل أو تُعالج البيانات خارج المملكة.

## 8. الاحتفاظ

مدد الاحتفاظ النهائية **غير معتمدة بعد**.

سيتم الاحتفاظ بالبيانات فقط للمدة التي يبررها الغرض والنظام ومتطلبات الأمان/التدقيق وجدول الاحتفاظ المعتمد.

القيم الزمنية المقترحة في وثيقة التوصيات لا تصبح التزامًا نهائيًا إلا بعد المراجعة القانونية والتشغيلية.

## 9. حقوق صاحب البيانات

وفق النظام والاستثناءات المنطبقة، قد تشمل الحقوق:

- الحق في العلم;
- الوصول إلى البيانات;
- الحصول على نسخة مقروءة;
- طلب التصحيح أو الإكمال أو التحديث;
- طلب الإتلاف عند انطباقه;
- الرجوع عن الموافقة عندما تكون المعالجة قائمة على الموافقة.

يجب توفير قناة عملية لممارسة الحقوق قبل الإطلاق الإنتاجي.

راجع `docs/legal/DATA_DELETION.md`.

## 10. النقل خارج المملكة

ما إذا كانت بيانات TYFINO ستُنقل أو تُخزن أو تُعالج خارج المملكة ما زال **OPEN** حتى اعتماد بنية الاستضافة الإنتاجية.

يجب أن تعكس السياسة العامة النهائية الوضع الفعلي المعتمد.

## 11. التواصل

لطلبات الخصوصية وحقوق البيانات:

**[على المالك توفير وسيلة تواصل عامة للخصوصية]**

---

## Regulatory references reviewed

- Saudi PDPL / SDAIA knowledge center: https://dgp.sdaia.gov.sa/wps/portal/pdp/knowledgecenter/details/PDPL
- SDAIA privacy-policy preparation guidance: https://dgp.sdaia.gov.sa/wps/portal/pdp/knowledgecenter/details/ElaborationandDevelopingPrivacyPolicyGuideline/
- Google Play User Data policy: https://support.google.com/googleplay/android-developer/answer/10144311
- Google Play Data safety guidance: https://support.google.com/googleplay/android-developer/answer/10787469

The applicable law and live platform policy text control over this engineering draft.
