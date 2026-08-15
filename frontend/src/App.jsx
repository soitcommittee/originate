import { useEffect, useMemo, useState } from 'react'
import { api } from './api.js'

const currency = new Intl.NumberFormat('en-MY', { style: 'currency', currency: 'MYR', maximumFractionDigits: 0 })
const compactCurrency = new Intl.NumberFormat('en-MY', { style: 'currency', currency: 'MYR', notation: 'compact', maximumFractionDigits: 1 })
const statusOrder = ['SUBMITTED', 'SCORED', 'APPROVED', 'ACCEPTED', 'DISBURSED']
const statusLabels = {
  SUBMITTED: 'Submitted', SCORED: 'Scored', APPROVED: 'Approved', ACCEPTED: 'Accepted',
  DISBURSED: 'Disbursed', REJECTED: 'Rejected',
}
const viewMeta = {
  applications: { eyebrow: 'Loan operations', title: 'Application workspace', subtitle: 'Review and progress every origination from submission to disbursement.' },
  pipeline: { eyebrow: 'Portfolio workflow', title: 'Origination pipeline', subtitle: 'A live operational view of applications across each decision stage.' },
  customers: { eyebrow: 'Customer management', title: 'Borrower directory', subtitle: 'Customer profiles, affordability data and total lending exposure.' },
  reports: { eyebrow: 'Portfolio intelligence', title: 'Management reports', subtitle: 'Decision performance, loan book composition and recent activity.' },
}

function StatusBadge({ status }) {
  return <span className={`status status-${status.toLowerCase()}`}>{statusLabels[status] || status}</span>
}

function Avatar({ name, large = false }) {
  const initials = name.split(' ').map((part) => part[0]).slice(0, 2).join('')
  return <div className={`avatar ${large ? 'avatar-large' : ''}`}>{initials}</div>
}

function EmptyState({ title, copy }) {
  return <div className="empty-state"><span>◇</span><strong>{title}</strong><p>{copy}</p></div>
}

function MetricCard({ label, value, caption, tone = 'default' }) {
  return <article className={`metric-card metric-${tone}`}><div className="metric-label"><span>{label}</span><i /></div><strong>{value}</strong><small>{caption}</small></article>
}

function ApplicationsView({ loans, selected, selectedId, setSelectedId, onReload, onAction, busy, search, setSearch, statusFilter, setStatusFilter }) {
  const filtered = loans.filter((loan) => {
    const matchesText = `${loan.applicantName} ${loan.purpose} ${loan.id}`.toLowerCase().includes(search.toLowerCase())
    return matchesText && (statusFilter === 'ALL' || loan.status === statusFilter)
  })

  return <section className="workspace">
    <div className="list-panel">
      <div className="panel-heading">
        <div><h2>Applications</h2><p>{filtered.length} of {loans.length} records</p></div>
        <button className="icon-button" onClick={onReload} title="Refresh applications">↻</button>
      </div>
      <div className="list-tools">
        <label className="search-field"><span>⌕</span><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search name or purpose" /></label>
        <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
          <option value="ALL">All statuses</option>
          {[...statusOrder, 'REJECTED'].map((status) => <option key={status} value={status}>{statusLabels[status]}</option>)}
        </select>
      </div>
      <div className="loan-list">
        {filtered.length === 0 && <EmptyState title="No matching applications" copy="Try another search or status filter." />}
        {filtered.map((loan) => <button key={loan.id} className={`loan-row ${selectedId === loan.id ? 'selected' : ''}`} onClick={() => setSelectedId(loan.id)}>
          <Avatar name={loan.applicantName} />
          <div className="loan-copy"><strong>{loan.applicantName}</strong><span>LOAN-{String(loan.id).padStart(5, '0')} · {loan.purpose}</span></div>
          <div className="loan-amount"><strong>{currency.format(loan.requestedAmount)}</strong><StatusBadge status={loan.status} /></div>
        </button>)}
      </div>
    </div>

    <div className="detail-panel">
      {!selected ? <EmptyState title="Select an application" copy="Choose a record to review its workflow and next action." /> : <>
        <div className="detail-header">
          <div><p className="eyebrow">LOAN-{String(selected.id).padStart(5, '0')}</p><h2>{selected.applicantName}</h2><span className="record-meta">Updated {formatDate(selected.updatedAt)}</span></div>
          <StatusBadge status={selected.status} />
        </div>
        <div className={`timeline ${selected.status === 'REJECTED' ? 'timeline-rejected' : ''}`}>
          {statusOrder.map((status, index) => {
            const currentIndex = statusOrder.indexOf(selected.status)
            const complete = selected.status === 'REJECTED' ? index < 2 : index <= currentIndex
            return <div key={status} className={complete ? 'complete' : ''}><span>{complete ? '✓' : index + 1}</span><small>{statusLabels[status]}</small></div>
          })}
        </div>
        {selected.status === 'REJECTED' && <div className="rejection-banner"><strong>Application declined</strong><span>{selected.decisionNotes}</span></div>}
        <div className="detail-grid">
          <div><span>Requested amount</span><strong>{currency.format(selected.requestedAmount)}</strong></div>
          <div><span>Tenure</span><strong>{selected.tenureMonths} months</strong></div>
          <div><span>Credit score</span><strong>{selected.creditScore || 'Not scored'}</strong></div>
          <div><span>Interest rate</span><strong>{selected.interestRate ? `${selected.interestRate}% p.a.` : 'Pending'}</strong></div>
          <div className="wide"><span>Purpose</span><strong>{selected.purpose}</strong></div>
          {selected.decisionNotes && selected.status !== 'REJECTED' && <div className="wide"><span>Decision notes</span><strong>{selected.decisionNotes}</strong></div>}
        </div>
        <WorkflowActions loan={selected} busy={busy} onAction={onAction} />
      </>}
    </div>
  </section>
}

function WorkflowActions({ loan, busy, onAction }) {
  return <div className="actions">
    <div><h3>Next action</h3><p>Move this application through the controlled origination workflow.</p></div>
    <div className="action-buttons">
      {loan.status === 'SUBMITTED' && <button disabled={busy} onClick={() => onAction(() => api.scoreLoan(loan.id), 'Credit scoring completed.')}>Run scoring</button>}
      {loan.status === 'SCORED' && <><button className="danger-outline" disabled={busy} onClick={() => onAction(() => api.decideLoan(loan.id, 'REJECTED'), 'Application rejected.')}>Reject</button><button disabled={busy} onClick={() => onAction(() => api.decideLoan(loan.id, 'APPROVED'), 'Application approved.')}>Approve</button></>}
      {loan.status === 'APPROVED' && <button disabled={busy} onClick={() => onAction(() => api.acceptLoan(loan.id), 'Customer acceptance recorded.')}>Record acceptance</button>}
      {loan.status === 'ACCEPTED' && <button className="disburse" disabled={busy} onClick={() => onAction(() => api.disburseLoan(loan.id), 'Funds disbursed successfully.')}>Disburse funds</button>}
      {['REJECTED', 'DISBURSED'].includes(loan.status) && <span className="terminal">No further action required</span>}
    </div>
  </div>
}

function PipelineView({ loans, openApplication }) {
  const rejected = loans.filter((loan) => loan.status === 'REJECTED')
  return <div className="pipeline-view">
    <div className="section-heading"><div><h2>Live workflow</h2><p>Applications are grouped by their current processing stage.</p></div><span className="live-indicator"><i /> Live portfolio</span></div>
    <div className="pipeline-board">
      {statusOrder.map((status) => {
        const stageLoans = loans.filter((loan) => loan.status === status)
        const stageValue = stageLoans.reduce((sum, loan) => sum + Number(loan.requestedAmount), 0)
        return <section className={`pipeline-column column-${status.toLowerCase()}`} key={status}>
          <header><div><span>{statusLabels[status]}</span><b>{stageLoans.length}</b></div><small>{compactCurrency.format(stageValue)}</small></header>
          <div className="pipeline-cards">
            {stageLoans.length === 0 && <p className="column-empty">No applications</p>}
            {stageLoans.map((loan) => <button className="pipeline-card" key={loan.id} onClick={() => openApplication(loan.id)}>
              <div><strong>{loan.applicantName}</strong><span>LOAN-{String(loan.id).padStart(5, '0')}</span></div>
              <b>{currency.format(loan.requestedAmount)}</b>
              <p>{loan.purpose}</p>
              <footer><span>{loan.creditScore ? `Score ${loan.creditScore}` : 'Awaiting score'}</span><i>→</i></footer>
            </button>)}
          </div>
        </section>
      })}
    </div>
    {rejected.length > 0 && <section className="declined-strip"><div><strong>Declined applications</strong><span>{rejected.length} records retained for audit and reporting</span></div><div>{rejected.map((loan) => <button key={loan.id} onClick={() => openApplication(loan.id)}>{loan.applicantName}<StatusBadge status="REJECTED" /></button>)}</div></section>}
  </div>
}

function CustomersView({ customers, loans, search, setSearch, onNewCustomer, openApplication }) {
  const filtered = customers.filter((customer) => `${customer.fullName} ${customer.email} ${customer.phone}`.toLowerCase().includes(search.toLowerCase()))
  const metrics = new Map(customers.map((customer) => {
    const customerLoans = loans.filter((loan) => loan.customerId === customer.id)
    return [customer.id, {
      applications: customerLoans.length,
      exposure: customerLoans.filter((loan) => loan.status !== 'REJECTED').reduce((sum, loan) => sum + Number(loan.requestedAmount), 0),
      latest: customerLoans[0],
    }]
  }))

  return <section className="directory-panel">
    <div className="directory-toolbar">
      <div><h2>Customer portfolio</h2><p>{customers.length} verified borrower profiles</p></div>
      <div><label className="search-field"><span>⌕</span><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search customers" /></label><button className="secondary-action" onClick={onNewCustomer}>+ Add customer</button></div>
    </div>
    <div className="customer-table-wrap">
      <table className="customer-table">
        <thead><tr><th>Customer</th><th>Contact</th><th>Monthly income</th><th>Applications</th><th>Total exposure</th><th>Latest status</th></tr></thead>
        <tbody>{filtered.map((customer) => {
          const customerMetric = metrics.get(customer.id)
          return <tr key={customer.id} onClick={() => customerMetric.latest && openApplication(customerMetric.latest.id)} className={customerMetric.latest ? 'clickable' : ''}>
            <td><div className="customer-name"><Avatar name={customer.fullName} /><div><strong>{customer.fullName}</strong><span>CUS-{String(customer.id).padStart(5, '0')}</span></div></div></td>
            <td><strong className="table-primary">{customer.email}</strong><span>{customer.phone}</span></td>
            <td><strong className="table-primary">{currency.format(customer.monthlyIncome)}</strong><span>Verified income</span></td>
            <td><strong className="table-primary">{customerMetric.applications}</strong><span>origination records</span></td>
            <td><strong className="table-primary">{currency.format(customerMetric.exposure)}</strong><span>excluding declined</span></td>
            <td>{customerMetric.latest ? <StatusBadge status={customerMetric.latest.status} /> : <span className="muted">No application</span>}</td>
          </tr>
        })}</tbody>
      </table>
      {filtered.length === 0 && <EmptyState title="No customers found" copy="Try another name, email address or phone number." />}
    </div>
  </section>
}

function ReportsView({ loans }) {
  const totalValue = loans.reduce((sum, loan) => sum + Number(loan.requestedAmount), 0)
  const approvedLoans = loans.filter((loan) => ['APPROVED', 'ACCEPTED', 'DISBURSED'].includes(loan.status))
  const disbursedLoans = loans.filter((loan) => loan.status === 'DISBURSED')
  const decided = loans.filter((loan) => ['APPROVED', 'ACCEPTED', 'DISBURSED', 'REJECTED'].includes(loan.status))
  const approvalRate = decided.length ? Math.round((approvedLoans.length / decided.length) * 100) : 0
  const averageTicket = loans.length ? totalValue / loans.length : 0
  const statusCounts = [...statusOrder, 'REJECTED'].map((status) => ({ status, count: loans.filter((loan) => loan.status === status).length }))
  const maxStatus = Math.max(...statusCounts.map((entry) => entry.count), 1)
  const purposeGroups = Object.entries(loans.reduce((groups, loan) => {
    const category = classifyPurpose(loan.purpose)
    groups[category] = (groups[category] || 0) + Number(loan.requestedAmount)
    return groups
  }, {})).sort((a, b) => b[1] - a[1])

  return <div className="reports-view">
    <section className="report-kpis">
      <MetricCard label="Approval rate" value={`${approvalRate}%`} caption={`${approvedLoans.length} of ${decided.length} decided applications`} tone="green" />
      <MetricCard label="Disbursed portfolio" value={currency.format(disbursedLoans.reduce((sum, loan) => sum + Number(loan.requestedAmount), 0))} caption={`${disbursedLoans.length} completed facilities`} tone="blue" />
      <MetricCard label="Average request" value={currency.format(averageTicket)} caption={`Across ${loans.length} applications`} tone="gold" />
      <MetricCard label="Portfolio requested" value={currency.format(totalValue)} caption="Gross requested principal" />
    </section>
    <div className="report-grid">
      <section className="report-card">
        <div className="card-heading"><div><h2>Application distribution</h2><p>Current volume by workflow stage</p></div><span>Count</span></div>
        <div className="bar-chart">{statusCounts.map(({ status, count }) => <div className="bar-row" key={status}><label>{statusLabels[status]}</label><div><i className={`bar-${status.toLowerCase()}`} style={{ width: `${Math.max((count / maxStatus) * 100, count ? 8 : 0)}%` }} /></div><strong>{count}</strong></div>)}</div>
      </section>
      <section className="report-card">
        <div className="card-heading"><div><h2>Loan book composition</h2><p>Requested value grouped by purpose</p></div><span>MYR</span></div>
        <div className="composition-list">{purposeGroups.map(([purpose, value], index) => <div key={purpose}><span className={`purpose-dot dot-${index % 5}`} /><label>{purpose}</label><div><i style={{ width: `${totalValue ? (value / totalValue) * 100 : 0}%` }} /></div><strong>{compactCurrency.format(value)}</strong></div>)}</div>
      </section>
      <section className="report-card report-activity">
        <div className="card-heading"><div><h2>Recent portfolio activity</h2><p>Latest application state changes</p></div><span>Last {Math.min(loans.length, 6)}</span></div>
        <div className="activity-list">{[...loans].sort((a, b) => new Date(b.updatedAt) - new Date(a.updatedAt)).slice(0, 6).map((loan) => <div key={loan.id}><Avatar name={loan.applicantName} /><div><strong>{loan.applicantName}</strong><span>LOAN-{String(loan.id).padStart(5, '0')} · {loan.purpose}</span></div><StatusBadge status={loan.status} /><time>{formatDate(loan.updatedAt)}</time></div>)}</div>
      </section>
    </div>
  </div>
}

function App() {
  const [activeView, setActiveView] = useState('applications')
  const [customers, setCustomers] = useState([])
  const [loans, setLoans] = useState([])
  const [selectedId, setSelectedId] = useState(null)
  const [showLoanForm, setShowLoanForm] = useState(false)
  const [showCustomerForm, setShowCustomerForm] = useState(false)
  const [busy, setBusy] = useState(false)
  const [notice, setNotice] = useState(null)
  const [applicationSearch, setApplicationSearch] = useState('')
  const [customerSearch, setCustomerSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')

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
    } catch (error) { showError(error) }
  }

  useEffect(() => { load() }, [])

  function showError(error) {
    setNotice({ type: 'error', title: error.code || 'REQUEST_FAILED', message: error.message, requestId: error.requestId })
  }

  async function runAction(action, successMessage) {
    setBusy(true); setNotice(null)
    try {
      const updated = await action()
      setLoans((items) => items.map((item) => item.id === updated.id ? updated : item))
      setNotice({ type: 'success', title: 'Workflow updated', message: successMessage })
    } catch (error) { showError(error) } finally { setBusy(false) }
  }

  async function createLoan(event) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    setBusy(true); setNotice(null)
    try {
      const created = await api.createLoan({ customerId: Number(form.get('customerId')), requestedAmount: Number(form.get('requestedAmount')), tenureMonths: Number(form.get('tenureMonths')), purpose: form.get('purpose') })
      setLoans((items) => [created, ...items]); setSelectedId(created.id); setActiveView('applications'); setShowLoanForm(false)
      setNotice({ type: 'success', title: 'Application submitted', message: `LOAN-${String(created.id).padStart(5, '0')} entered the workflow.` })
    } catch (error) { showError(error) } finally { setBusy(false) }
  }

  async function createCustomer(event) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    setBusy(true); setNotice(null)
    try {
      const created = await api.createCustomer({ fullName: form.get('fullName'), email: form.get('email'), phone: form.get('phone'), monthlyIncome: Number(form.get('monthlyIncome')) })
      setCustomers((items) => [...items, created]); setShowCustomerForm(false)
      setNotice({ type: 'success', title: 'Customer created', message: `${created.fullName} is ready for a new application.` })
    } catch (error) { showError(error) } finally { setBusy(false) }
  }

  function openApplication(id) { setSelectedId(id); setActiveView('applications') }
  const meta = viewMeta[activeView]

  return <div className="app-shell">
    <aside className="sidebar">
      <div className="brand"><span className="brand-mark">O</span><div><span>Originate</span><small>Loan operations</small></div></div>
      <nav>{[
        ['applications', '▦', 'Applications'], ['pipeline', '⌁', 'Pipeline'], ['customers', '◎', 'Customers'], ['reports', '▥', 'Reports'],
      ].map(([view, icon, label]) => <button key={view} className={activeView === view ? 'active' : ''} onClick={() => setActiveView(view)}><span>{icon}</span>{label}{view === 'applications' && <b>{loans.length}</b>}</button>)}</nav>
      <div className="sidebar-divider" />
      <div className="quick-summary"><p>Portfolio snapshot</p><div><span>Active</span><strong>{counts.active}</strong></div><div><span>Requested</span><strong>{compactCurrency.format(counts.value)}</strong></div></div>
      <div className="environment-card"><span className="pulse" /><div><strong>Production · Malaysia</strong><small>6 services · PostgreSQL</small></div></div>
    </aside>

    <main>
      <header className="topbar">
        <div><p className="eyebrow">{meta.eyebrow}</p><h1>{meta.title}</h1><p className="page-subtitle">{meta.subtitle}</p></div>
        <div className="topbar-actions"><span className="data-freshness"><i /> Live data</span>{['applications', 'pipeline'].includes(activeView) && <button className="primary" onClick={() => setShowLoanForm(true)}>+ New application</button>}</div>
      </header>

      {activeView === 'applications' && <section className="metrics">
        <MetricCard label="Active applications" value={counts.active} caption="Awaiting an operational action" tone="green" />
        <MetricCard label="Approved facilities" value={counts.approved} caption="Approved, accepted or disbursed" tone="blue" />
        <MetricCard label="Requested value" value={currency.format(counts.value)} caption="Across the complete portfolio" tone="gold" />
      </section>}

      {notice && <div className={`notice ${notice.type}`}><div><strong>{notice.title}</strong><span>{notice.message}</span>{notice.requestId && <code>Request ID: {notice.requestId}</code>}</div><button onClick={() => setNotice(null)}>×</button></div>}

      {activeView === 'applications' && <ApplicationsView loans={loans} selected={selected} selectedId={selectedId} setSelectedId={setSelectedId} onReload={load} onAction={runAction} busy={busy} search={applicationSearch} setSearch={setApplicationSearch} statusFilter={statusFilter} setStatusFilter={setStatusFilter} />}
      {activeView === 'pipeline' && <PipelineView loans={loans} openApplication={openApplication} />}
      {activeView === 'customers' && <CustomersView customers={customers} loans={loans} search={customerSearch} setSearch={setCustomerSearch} onNewCustomer={() => setShowCustomerForm(true)} openApplication={openApplication} />}
      {activeView === 'reports' && <ReportsView loans={loans} />}
    </main>

    {showLoanForm && <div className="modal-backdrop" onMouseDown={() => setShowLoanForm(false)}><form className="modal" onSubmit={createLoan} onMouseDown={(event) => event.stopPropagation()}>
      <div className="modal-title"><div><p className="eyebrow">New origination</p><h2>Create application</h2><span>Capture the core facility request and start the workflow.</span></div><button type="button" onClick={() => setShowLoanForm(false)}>×</button></div>
      <label>Customer<select name="customerId" required defaultValue=""><option value="" disabled>Select a verified customer</option>{customers.map((customer) => <option key={customer.id} value={customer.id}>{customer.fullName} · {currency.format(customer.monthlyIncome)}/month</option>)}</select></label>
      <div className="form-row"><label>Requested amount<input name="requestedAmount" type="number" min="1000" step="1000" defaultValue="50000" required /></label><label>Tenure<select name="tenureMonths" defaultValue="60"><option value="12">12 months</option><option value="36">36 months</option><option value="48">48 months</option><option value="60">60 months</option><option value="84">84 months</option><option value="120">120 months</option></select></label></div>
      <label>Loan purpose<input name="purpose" placeholder="e.g. Home renovation" required /></label>
      <div className="modal-actions"><button type="button" className="secondary" onClick={() => setShowLoanForm(false)}>Cancel</button><button className="primary" disabled={busy}>Submit application</button></div>
    </form></div>}

    {showCustomerForm && <div className="modal-backdrop" onMouseDown={() => setShowCustomerForm(false)}><form className="modal" onSubmit={createCustomer} onMouseDown={(event) => event.stopPropagation()}>
      <div className="modal-title"><div><p className="eyebrow">Customer onboarding</p><h2>Add borrower profile</h2><span>Create an affordability profile before originating a facility.</span></div><button type="button" onClick={() => setShowCustomerForm(false)}>×</button></div>
      <label>Full name<input name="fullName" placeholder="e.g. Sarah Lee" required /></label>
      <div className="form-row"><label>Email address<input name="email" type="email" placeholder="sarah@example.com" required /></label><label>Phone number<input name="phone" placeholder="+60 12-345 6789" required /></label></div>
      <label>Verified monthly income<input name="monthlyIncome" type="number" min="1000" step="100" placeholder="8500" required /></label>
      <div className="modal-actions"><button type="button" className="secondary" onClick={() => setShowCustomerForm(false)}>Cancel</button><button className="primary" disabled={busy}>Create customer</button></div>
    </form></div>}
  </div>
}

function formatDate(value) {
  if (!value) return 'just now'
  return new Intl.DateTimeFormat('en-MY', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' }).format(new Date(value))
}

function classifyPurpose(purpose) {
  const value = purpose.toLowerCase()
  if (value.includes('home') || value.includes('property')) return 'Property'
  if (value.includes('business') || value.includes('capital') || value.includes('equipment')) return 'Business'
  if (value.includes('education') || value.includes('certification')) return 'Education'
  if (value.includes('vehicle')) return 'Vehicle'
  return 'Personal & other'
}

export default App
