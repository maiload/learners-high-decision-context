import http from 'k6/http';
import { uuidv4, randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

export const options = {
    scenarios: {
        normal: {
            executor: 'constant-arrival-rate',
            rate: 1000,
            timeUnit: '1s',
            duration: '1m',
            preAllocatedVUs: 30,
            maxVUs: 50,
        },
    },
};

const BASE_URL = 'http://localhost:8080';

function generateDecisionLog() {
    const decisionId = uuidv4();
    const timestamp = new Date().toISOString();

    return {
        path: 'cloud_access/device_posture/response',
        input: {
            agent_data: {
                data: {
                    os_type: 'Windows',
                    file_list: [{ path: '/etc/vaccine' }],
                    process_list: [{ name: 'antivirus', path: 'C:\\Program Files\\Antivirus\\av.exe' }],
                    registry_list: [],
                    antivirus_list: [{ display_name: 'security_center' }],
                    client_ip_hash_list: ['ip_hash'],
                    client_mac_hash_list: ['mac_hash'],
                },
                event_type: 'device_posture_reported',
            },
            access_token: 'test_token_' + decisionId,
        },
        labels: {
            id: uuidv4(),
            version: '1.10.1',
        },
        req_id: randomIntBetween(1, 10000),
        result: {
            allow: true,
            score: {
                grade: 'A',
                score: 100,
                max_score: 100,
                threshold: 70,
                risk_level: 'low',
                passes_threshold: true,
            },
            policies: [
                {
                    policy_name: 'vaccine_policy',
                    policy_data: { allow: true, violations: [] },
                },
            ],
            access_key: {
                user_id: uuidv4(),
                realm_id: uuidv4(),
                user_policy_id: uuidv4(),
            },
            violations: [],
        },
        bundles: {},
        timestamp: timestamp,
        decision_id: decisionId,
        requested_by: '192.168.1.100',
    };
}

export default function () {
    const batchSize = randomIntBetween(1, 5);
    const payload = [];

    for (let i = 0; i < batchSize; i++) {
        payload.push(generateDecisionLog());
    }

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
        compression: 'gzip',
    };

    const response = http.post(
        `${BASE_URL}/logs`,
        JSON.stringify(payload),
        params
    );


    if (response.status !== 204) {
        console.log(`Status: ${response.status}, Body: ${response.body}`);
    }
}
