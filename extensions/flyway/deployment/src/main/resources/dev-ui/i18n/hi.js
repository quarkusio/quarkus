import { str } from '@lit/localize';

export const templates = {
    'quarkus-flyway-meta-description': 'अपने डेटाबेस स्कीमा माइग्रेशन संभालें',
    'quarkus-flyway-datasources': 'डेटा स्रोत',
    'quarkus-flyway-name': 'नाम',
    'quarkus-flyway-action': 'क्रिया',
    'quarkus-flyway-clean': 'साफ करें',
    'quarkus-flyway-migrate': 'स्थानांतरित करें',
    'quarkus-flyway-clean-disabled-tooltip': 'Flyway क्लीन को quarkus.flyway.clean-disabled=true के माध्यम से निष्क्रिय कर दिया गया है।',
    'quarkus-flyway-update-button-title': 'अपडेट माइग्रेशन फ़ाइल बनाएँ। हमेशा बनाई गई फ़ाइल का मैन्युअल रूप से समीक्षा करें क्योंकि यह डेटा हानि का कारण बन सकती है।',
    'quarkus-flyway-generate-migration-file': 'माइग्रेशन फ़ाइल उत्पन्न करें',
    'quarkus-flyway-create-button-title': 'Flyway माइग्रेशन के लिए मूल फ़ाइलें सेट करें। db/migrations में प्रारंभिक फ़ाइल बनाई जाएगी और आप फिर अतिरिक्त माइग्रेशन फ़ाइलें जोड़ सकते हैं।',
    'quarkus-flyway-create-initial-migration-file': 'प्रारंभिक माइग्रेशन फ़ाइल बनाएं',
    'quarkus-flyway-create': 'बनाएँ',
    'quarkus-flyway-update': 'अपडेट करें',
    'quarkus-flyway-create-dialog-description': 'Flyway माइग्रेशन के लिए Hibernate ORM स्कीमा जनरेशन से एक प्रारंभिक फ़ाइल सेट करें।<br/>यदि आप हां कहते हैं, तो एक प्रारंभिक फ़ाइल <code>db/migrations</code> में <br/> बनाई जाएगी और फिर आप दस्तावेज़ के अनुसार अतिरिक्त माइग्रेशन फ़ाइलें जोड़ सकते हैं।',
    'quarkus-flyway-update-dialog-description': 'Hibernate ORM स्कीमा डिफ से एक वृद्धिशील माइग्रेशन फ़ाइल बनाएं।<br/>यदि आप हाँ कहते हैं, तो एक अतिरिक्त फ़ाइल <code>db/migrations</code> में <br/>निर्मित की जाएगी।',
    'quarkus-flyway-cancel': 'रद्द करें',
    'quarkus-flyway-clean-confirm': 'यह सभी ऑब्जेक्ट्स (टेबल, व्यू, प्रक्रियाएँ, ट्रिगर्स, ...) को निर्धारित स्कीमा में हटा देगा। क्या आप जारी रखना चाहते हैं?',
    'quarkus-flyway-datasource-title': str`${0} डेटा स्रोत`,
};
