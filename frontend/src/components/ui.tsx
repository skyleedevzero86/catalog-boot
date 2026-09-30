import type { ButtonHTMLAttributes, ReactNode } from 'react'

const variants = {
  primary: 'bg-indigo-600 hover:bg-indigo-500 text-white',
  secondary: 'bg-slate-700 hover:bg-slate-600 text-slate-100',
  danger: 'bg-rose-700 hover:bg-rose-600 text-white',
  ghost: 'bg-transparent hover:bg-slate-800 text-slate-300',
} as const

type Props = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: keyof typeof variants
  loading?: boolean
}

export function Button({
  variant = 'primary',
  loading,
  disabled,
  className = '',
  children,
  ...rest
}: Props) {
  return (
    <button
      type="button"
      disabled={disabled || loading}
      className={`inline-flex items-center justify-center gap-2 rounded-lg px-4 py-2 text-sm font-medium transition disabled:opacity-50 ${variants[variant]} ${className}`}
      {...rest}
    >
      {loading ? '처리 중…' : children}
    </button>
  )
}

export function Card({
  title,
  children,
  action,
}: {
  title?: string
  children: ReactNode
  action?: ReactNode
}) {
  return (
    <section className="rounded-xl border border-slate-800 bg-slate-900/60 p-5 shadow-lg">
      {(title || action) && (
        <div className="mb-4 flex items-center justify-between gap-3">
          {title && (
            <h2 className="text-lg font-semibold text-slate-100">{title}</h2>
          )}
          {action}
        </div>
      )}
      {children}
    </section>
  )
}

export function Input({
  label,
  className = '',
  ...props
}: React.InputHTMLAttributes<HTMLInputElement> & { label?: string }) {
  return (
    <label className="block text-sm">
      {label && (
        <span className="mb-1 block text-slate-400">{label}</span>
      )}
      <input
        className={`w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 outline-none focus:border-indigo-500 ${className}`}
        {...props}
      />
    </label>
  )
}

export function Select({
  label,
  options,
  className = '',
  ...props
}: React.SelectHTMLAttributes<HTMLSelectElement> & {
  label?: string
  options: { value: string; label: string }[]
}) {
  return (
    <label className="block text-sm">
      {label && (
        <span className="mb-1 block text-slate-400">{label}</span>
      )}
      <select
        className={`w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 outline-none focus:border-indigo-500 ${className}`}
        {...props}
      >
        {options.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
    </label>
  )
}

export function Textarea({
  label,
  className = '',
  ...props
}: React.TextareaHTMLAttributes<HTMLTextAreaElement> & {
  label?: string
}) {
  return (
    <label className="block text-sm">
      {label && (
        <span className="mb-1 block text-slate-400">{label}</span>
      )}
      <textarea
        className={`w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 font-mono text-sm text-slate-100 outline-none focus:border-indigo-500 ${className}`}
        {...props}
      />
    </label>
  )
}

export function Badge({
  children,
  tone = 'default',
}: {
  children: ReactNode
  tone?: 'default' | 'success' | 'warn' | 'error' | 'info'
}) {
  const tones = {
    default: 'bg-slate-700 text-slate-200',
    success: 'bg-emerald-900/80 text-emerald-300',
    warn: 'bg-amber-900/80 text-amber-300',
    error: 'bg-rose-900/80 text-rose-300',
    info: 'bg-indigo-900/80 text-indigo-300',
  }
  return (
    <span
      className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium ${tones[tone]}`}
    >
      {children}
    </span>
  )
}

export function Alert({
  type = 'info',
  children,
}: {
  type?: 'info' | 'error' | 'success'
  children: ReactNode
}) {
  const styles = {
    info: 'border-indigo-800 bg-indigo-950/50 text-indigo-200',
    error: 'border-rose-800 bg-rose-950/50 text-rose-200',
    success: 'border-emerald-800 bg-emerald-950/50 text-emerald-200',
  }
  return (
    <div className={`rounded-lg border px-4 py-3 text-sm ${styles[type]}`}>
      {children}
    </div>
  )
}

export function statusTone(
  status: string,
): 'default' | 'success' | 'warn' | 'error' | 'info' {
  const s = status.toUpperCase()
  if (['SUCCESS', 'COMPLETED', 'PREPARED', 'HEALTHY'].includes(s))
    return 'success'
  if (['RUNNING', 'EXPORTING', 'PENDING', 'VALIDATING'].includes(s))
    return 'info'
  if (['PARTIAL_SUCCESS', 'PREPARING'].includes(s)) return 'warn'
  if (['FAILED', 'UNHEALTHY', 'CANCELLED'].includes(s)) return 'error'
  return 'default'
}
