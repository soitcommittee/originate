import { useEffect, useMemo, useState } from 'react'
import { api } from './api.js'

const currency = new Intl.NumberFormat('en-MY', { style: 'currency', currency: 'MYR', maximumFractionDigits: 0 })
const statusOrder = ['SUBMITTED', 'SCORED', 'APPROVED', 'ACCEPTED', 'DISBURSED']

function StatusBadge({ status }) {
  return <span className={`status status-${status.toLowerCase()}`}>{status.replace('_', ' ')}</span>
}

function App() {
  const [customers, setCustomers] = useState([])
  const [loans, setLoans] = useState([])
  const [selectedId, setSelectedId] = useState(null)
  const [showForm, setShowForm] = useState(false)
  const [busy, setBusy] = useState(false)
  const [notice, setNotice] = useState(null)

  const selected = loans.find((loan) => loan.id === selectedId) || loans[0]
  const counts = useMemo(() => ({
    active: loans.filter((loan) => !['REJECTED', 'DISBURSED'].includes(loan.status)).length,
    approved: loans.filter((loan) => ['APPROVED', 'ACCEPTED', 'DISBURSED'].includes(loan.status)).length,
    value: loans.reduce((sum, loan) => sum + Number(loan.requestedAmount), 0),
  }), [loans])

  async function load() {
    try {
      const [customerData, loanData] = await Promise.all([api.getCustomers(), api.getLoans()])
      setCustomers(customerData)
      setLoans(loanData)
      setSelectedId((current) => current || loanData[0]?.id || null)
    } catch (error) {
      showError(error)
    }
  }

  useEffect(() => { load() }, [])

  function showError(error) {
    setNotice({ type: 'error', title: error.code || 'REQUEST_FAILED', message: error.message, requestId: error.requestId })
  }

  async function runAction(action, successMessage) {
    setBusy(true)
    setNotice(null)
    try {
      const updated = await action()
      setLoans((items) => items.map((item) => item.id === updated.id ? updated : item))
      setNotice({ type: 'success', title: 'Workflow updated', message: successMessage })
    } catch (error) {
      showError(error)
    } finally {
      setBusy(false)
    }
  }

  async function createLoan(event) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    setBusy(true)
    try {
      const created = await api.createLoan({
        customerId: Number(form.get('customerId')),
        requestedAmount: Number(form.get('requestedAmount')),
        tenureMonths: Number(form.get('tenureMonths')),
        purpose: form.get('purpose'),
      })
      setLoans((items) => [created, ...items])
      setSelectedId(created.id)
      setShowForm(false)
      setNotice({ type: 'success', title: 'Application submitted', message: `Application LOAN-${String(created.id).padStart(5, '0')} entered the workflow.` })
    } catch (error) {
      showError(error)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand"><span className="brand-mark">O</span><span>Originate</span></div>
        <nav>
          <a className="active" href="#applications"><span>◫</span> Applications</a>
          <a href="#pipeline"><span>⌁</span> Pipeline</a>
          <a href="#customers"><span>◎</span> Customers</a>
          <a href="#reports"><span>▥</span> Reports</a>
        </nav>
        <div className="environment-card">
          <span className="pulse" />
          <div><strong>Originate platform</strong><small>6 services expected</small></div>
        </div>
      </aside>

      <main>
        <header className="topbar">
          <div><p className="eyebrow">Loan operations</p><h1>Application workspace</h1></div>
          <button className="primary" onClick={() => setShowForm(true)}>+ New application</button>
        </header>

        <section className="metrics">
          <article><span>Active applications</span><strong>{counts.active}</strong><small>Awaiting action</small></article>
          <article><span>Approved</span><strong>{counts.approved}</strong><small>Including disbursed</small></article>
          <article><span>Requested value</span><strong>{currency.format(counts.value)}</strong><small>Across all applications</small></article>
        </section>

        {notice && (
          <div className={`notice ${notice.type}`}>
            <div><strong>{notice.title}</strong><span>{notice.message}</span>{notice.requestId && <code>Request ID: {notice.requestId}</code>}</div>
            <button onClick={() => setNotice(null)}>×</button>
          </div>
        )}

        <section className="workspace" id="applications">
          <div className="list-panel">
            <div className="panel-heading"><div><h2>Applications</h2><p>{loans.length} total records</p></div><button className="icon-button" onClick={load}>↻</button></div>
            <div className="loan-list">
              {loans.length === 0 && <div className="empty">No applications yet.<br />Create one to begin.</div>}
              {loans.map((loan) => (
                <button key={loan.id} className={`loan-row ${selected?.id === loan.id ? 'selected' : ''}`} onClick={() => setSelectedId(loan.id)}>
                  <div className="avatar">{loan.applicantName.split(' ').map((part) => part[0]).slice(0, 2).join('')}</div>
                  <div className="loan-copy"><strong>{loan.applicantName}</strong><span>LOAN-{String(loan.id).padStart(5, '0')} · {loan.purpose}</span></div>
                  <div className="loan-amount"><strong>{currency.format(loan.requestedAmount)}</strong><StatusBadge status={loan.status} /></div>
                </button>
              ))}
            </div>
          </div>

          <div className="detail-panel">
            {!selected ? <div className="empty detail-empty">Select an application to view its workflow.</div> : <>
              <div className="detail-header">
                <div><p className="eyebrow">LOAN-{String(selected.id).padStart(5, '0')}</p><h2>{selected.applicantName}</h2></div>
                <StatusBadge status={selected.status} />
              </div>

              <div className="timeline">
                {statusOrder.map((status, index) => {
                  const currentIndex = statusOrder.indexOf(selected.status)
                  const completed = selected.status === 'REJECTED' ? index < 2 : index <= currentIndex
                  return <div key={status} className={completed ? 'complete' : ''}><span>{completed ? '✓' : index + 1}</span><small>{status}</small></div>
                })}
              </div>

              <div className="detail-grid">
                <div><span>Requested amount</span><strong>{currency.format(selected.requestedAmount)}</strong></div>
                <div><span>Tenure</span><strong>{selected.tenureMonths} months</strong></div>
                <div><span>Credit score</span><strong>{selected.creditScore || 'Not scored'}</strong></div>
                <div><span>Interest rate</span><strong>{selected.interestRate ? `${selected.interestRate}% p.a.` : 'Pending'}</strong></div>
                <div className="wide"><span>Purpose</span><strong>{selected.purpose}</strong></div>
                {selected.decisionNotes && <div className="wide"><span>Decision notes</span><strong>{selected.decisionNotes}</strong></div>}
              </div>

              <div className="actions">
                <div><h3>Next action</h3><p>Move this application through the origination workflow.</p></div>
                <div className="action-buttons">
                  {selected.status === 'SUBMITTED' && <button disabled={busy} onClick={() => runAction(() => api.scoreLoan(selected.id), 'Credit scoring completed.')}>Run scoring</button>}
                  {selected.status === 'SCORED' && <><button className="danger-outline" disabled={busy} onClick={() => runAction(() => api.decideLoan(selected.id, 'REJECTED'), 'Application rejected.')}>Reject</button><button disabled={busy} onClick={() => runAction(() => api.decideLoan(selected.id, 'APPROVED'), 'Application approved.')}>Approve</button></>}
                  {selected.status === 'APPROVED' && <button disabled={busy} onClick={() => runAction(() => api.acceptLoan(selected.id), 'Customer acceptance recorded.')}>Record acceptance</button>}
                  {selected.status === 'ACCEPTED' && <button className="disburse" disabled={busy} onClick={() => runAction(() => api.disburseLoan(selected.id), 'Funds disbursed successfully.')}>Disburse funds</button>}
                  {['REJECTED', 'DISBURSED'].includes(selected.status) && <span className="terminal">No further action required</span>}
                </div>
              </div>
            </>}
          </div>
        </section>
      </main>

      {showForm && <div className="modal-backdrop" onMouseDown={() => setShowForm(false)}>
        <form className="modal" onSubmit={createLoan} onMouseDown={(event) => event.stopPropagation()}>
          <div className="modal-title"><div><p className="eyebrow">New origination</p><h2>Create application</h2></div><button type="button" onClick={() => setShowForm(false)}>×</button></div>
          <label>Customer<select name="customerId" required defaultValue=""><option value="" disabled>Select a customer</option>{customers.map((customer) => <option key={customer.id} value={customer.id}>{customer.fullName} · {currency.format(customer.monthlyIncome)}/month</option>)}</select></label>
          <div className="form-row"><label>Requested amount<input name="requestedAmount" type="number" min="1000" step="1000" defaultValue="50000" required /></label><label>Tenure<select name="tenureMonths" defaultValue="60"><option value="12">12 months</option><option value="36">36 months</option><option value="60">60 months</option><option value="120">120 months</option></select></label></div>
          <label>Loan purpose<input name="purpose" placeholder="e.g. Home renovation" required /></label>
          <div className="modal-actions"><button type="button" className="secondary" onClick={() => setShowForm(false)}>Cancel</button><button className="primary" disabled={busy}>Submit application</button></div>
        </form>
      </div>}
    </div>
  )
}

export default App
