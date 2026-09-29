// Steady background traffic for the demo: ~2 requests/second through the gateway.
//   docker compose --profile load up -d k6
import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const SKUS = ['JOLLOF-001', 'SUYA-001', 'PUFF-001', 'CHIN-001'];

export const options = {
    scenarios: {
        orders: {
            executor: 'constant-arrival-rate',
            rate: 2,
            timeUnit: '1s',
            duration: '4h',
            preAllocatedVUs: 10,
            maxVUs: 50,
        },
    },
};

const pick = (items) => items[Math.floor(Math.random() * items.length)];

// W3C baggage: 10% gold, 30% silver, 60% standard.
function customerTier() {
    const r = Math.random();
    return r < 0.1 ? 'gold' : r < 0.4 ? 'silver' : 'standard';
}

export default function () {
    const headers = {
        'Content-Type': 'application/json',
        baggage: `customer.tier=${customerTier()}`,
    };
    const r = Math.random();

    if (r < 0.10) {
        const res = http.get(`${BASE_URL}/api/inventory/${pick(SKUS)}`, { headers });
        check(res, { 'stock 200': (x) => x.status === 200 });
    } else if (r < 0.15) {
        // Unknown SKU -> 404 from inventory, surfaced by order-service
        http.post(`${BASE_URL}/api/orders`,
            JSON.stringify({ sku: 'SHAWARMA-001', quantity: 1, customerEmail: 'k6@naijajug.ng' }), { headers });
    } else {
        const res = http.post(`${BASE_URL}/api/orders`,
            JSON.stringify({ sku: pick(SKUS), quantity: 1 + Math.floor(Math.random() * 3), customerEmail: 'k6@naijajug.ng' }),
            { headers });
        check(res, { 'order 201': (x) => x.status === 201 });
    }
}
