const CUSTOMER_API = import.meta.env.VITE_CUSTOMER_API_URL || 'http://localhost:8081/api/customers'
const LOAN_API = import.meta.env.VITE_LOAN_API_URL || 'http://localhost:8082/api/loan-applications'

async function request(url, options = {}) {
  const requestId = crypto.randomUUID()
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

