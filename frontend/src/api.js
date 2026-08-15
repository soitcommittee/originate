const CUSTOMER_API = import.meta.env.VITE_CUSTOMER_API_URL || '/api/customers'
const LOAN_API = import.meta.env.VITE_LOAN_API_URL || '/api/loan-applications'

function createRequestId() {
  const cryptoApi = globalThis.crypto

  if (typeof cryptoApi?.randomUUID === 'function') {
    return cryptoApi.randomUUID()
  }

  if (typeof cryptoApi?.getRandomValues === 'function') {
    const bytes = cryptoApi.getRandomValues(new Uint8Array(16))
    bytes[6] = (bytes[6] & 0x0f) | 0x40
    bytes[8] = (bytes[8] & 0x3f) | 0x80
    const value = Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('')
    return `${value.slice(0, 8)}-${value.slice(8, 12)}-${value.slice(12, 16)}-${value.slice(16, 20)}-${value.slice(20)}`
  }

  return `web-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 12)}`
}

async function request(url, options = {}) {
  const requestId = createRequestId()
  const response = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      'X-Request-ID': requestId,
      ...options.headers,
    },
  })

  const body = await response.json().catch(() => null)
  if (!response.ok) {
    const error = new Error(body?.message || `Request failed with status ${response.status}`)
    error.code = body?.errorCode
    error.requestId = body?.requestId || response.headers.get('X-Request-ID') || requestId
    throw error
  }
  return body
}

export const api = {
  getCustomers: () => request(CUSTOMER_API),
  getLoans: () => request(LOAN_API),
  createLoan: (loan) => request(LOAN_API, { method: 'POST', body: JSON.stringify(loan) }),
  scoreLoan: (id) => request(`${LOAN_API}/${id}/score`, { method: 'POST' }),
  decideLoan: (id, decision) => request(`${LOAN_API}/${id}/decision`, {
    method: 'POST',
    body: JSON.stringify({ decision, notes: decision === 'APPROVED' ? 'Affordability and credit policy checks passed.' : 'Credit policy threshold not met.' }),
  }),
  acceptLoan: (id) => request(`${LOAN_API}/${id}/accept`, { method: 'POST' }),
  disburseLoan: (id) => request(`${LOAN_API}/${id}/disburse`, { method: 'POST' }),
}
