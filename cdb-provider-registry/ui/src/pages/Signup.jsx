import React, { useState, useRef } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { registerProvider, precheckRegistration } from '../services/providerService.js'
import { requestOtp } from '../services/otpService.js'
import { validateProviderRegistration } from '../validation/providerValidation.js'
import './Signup.css'

export default function Signup() {
  const navigate = useNavigate()

  // Step control
  const [step, setStep] = useState(1)

  // Provider fields
  const [name, setName] = useState('Acme Healthcare Solutions')
  const [code, setCode] = useState('acme-healthcare')
  const [contactEmail, setContactEmail] = useState('contact@acme-healthcare.com')
  const [contactPhone, setContactPhone] = useState('+1 (555) 123-4567')
  const [tagInput, setTagInput] = useState('')

  // Admin user fields
  const [adminEmail, setAdminEmail] = useState('admin@acme-healthcare.com')
  const [adminPassword, setAdminPassword] = useState('password123')
  const [adminFirstName, setAdminFirstName] = useState('John')
  const [adminLastName, setAdminLastName] = useState('Doe')

  // OTP state
  const [otpSent, setOtpSent] = useState(false)
  const [otpKey, setOtpKey] = useState('')
  const [otp, setOtp] = useState('')
  const [otpVerified, setOtpVerified] = useState(false)

  // UI state
  const [errors, setErrors] = useState({})
  const [submitting, setSubmitting] = useState(false)
  const [serverError, setServerError] = useState('')
  const [showPassword, setShowPassword] = useState(false)

  const rteRef = useRef(null)

  const steps = [
    { number: 1, title: 'Provider Details', description: 'Basic information about your organization' },
    { number: 2, title: 'Admin User', description: 'Create an administrator account' },
    { number: 3, title: 'Verification', description: 'Verify your email address' }
  ]

  const runValidation = () => {
    const data = {
      name,
      code,
      contactEmail,
      contactPhone,
      adminUsername: adminEmail,
      adminEmail,
      adminPassword,
      adminFirstName,
      adminLastName,
    }
    const e = validateProviderRegistration(data)
    setErrors(e)
    return { ok: Object.keys(e).length === 0, data }
  }

  const nextFromStep1 = () => {
    // Basic checks for step 1 fields only
    const e = {}
    if (!name.trim()) e.name = 'Name is required'
    if (!code.trim()) e.code = 'Code is required'
    setErrors(e)
    if (Object.keys(e).length === 0) {
      // Prefill admin email with contact email when moving to step 2
      if (contactEmail && !adminEmail) {
        setAdminEmail(contactEmail)
      }
      setStep(2)
    }
  }

  const nextFromStep2 = async () => {
    setServerError('')
    const { ok, data } = runValidation()
    if (!ok) {
      console.log('Validation errors:', errors)
      setServerError('Please fix the validation errors below')
      return
    }
    try {
      const res = await precheckRegistration({ code, adminEmail })
      if (res.ok) {
        const key = await res.text()
        setOtpKey(key)
        setOtpSent(true)
        setStep(3)
      } else if (res.status === 409) {
        const msg = await res.text()
        setServerError(msg || 'Pre-check failed')
      } else {
        const msg = await res.text()
        setServerError(msg || 'Failed to initiate OTP. Please try again.')
      }
    } catch (err) {
      setServerError(err.message || 'Network error')
    }
  }

  const addTag = (val) => {
    const t = val.trim().replace(/,/g, '')
    if (!t) return
    if (!tags.includes(t)) setTags([...tags, t])
    setTagInput('')
  }

  const removeTag = (t) => setTags(tags.filter(x => x !== t))

  const handleTagKeyDown = (e) => {
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault()
      addTag(tagInput)
    } else if (e.key === 'Backspace' && !tagInput && tags.length) {
      removeTag(tags[tags.length - 1])
    }
  }

  const sendOtp = async () => {
    setServerError('')
    if (!adminEmail) {
      setServerError('Enter admin email to receive OTP')
      return
    }
    try {
      const key = await requestOtp(code, adminEmail)
      if (key) {
        setOtpKey(key)
        setOtpSent(true)
      } else {
        setServerError('Failed to send OTP')
      }
    } catch (err) {
      setServerError(err.message || 'Network error')
    }
  }

  const doVerifyOtp = async () => {
    setServerError('')
    if (!/^\d{6}$/.test(otp)) {
      setServerError('Enter 6-digit OTP')
      return
    }
    if (!otpKey) {
      setServerError('Missing OTP key. Please request OTP again.')
      return
    }
    try {
      // Directly attempt registration; backend will verify OTP server-side
      setOtpVerified(true)
      await completeRegistration()
    } catch (err) {
      setServerError(err.message || 'Network error')
    }
  }

  const completeRegistration = async () => {
    const { ok, data } = runValidation()
    if (!ok) {
      setStep(2)
      return
    }
    if (!otpKey || !otp) {
      setServerError('Please verify your OTP first')
      return
    }
    setSubmitting(true)
    try {
      const req = { ...data, otpKey, otp }
      const res = await registerProvider(req)
      if (res.status === 201) {
        navigate('/login', { replace: true, state: { registered: true, email: adminEmail } })
      } else if (res.status === 409) {
        setServerError('Provider code already exists. Please choose another code.')
      } else {
        const text = await res.text()
        setServerError(text || 'Registration failed. Please try again.')
      }
    } catch (err) {
      setServerError(err.message || 'Network error')
    } finally {
      setSubmitting(false)
    }
  }

  const StepperComponent = () => (
    <div className="stepper-container">
      <div className="stepper-header">
        <h1 className="stepper-title">Register your Provider</h1>
        <p className="stepper-subtitle">Join the CDB ecosystem and start providing services</p>
      </div>
      
      <div className="stepper">
        {steps.map((stepItem, index) => (
          <div key={stepItem.number} className="stepper-item">
            <div className={`stepper-step ${step >= stepItem.number ? 'active' : ''} ${step > stepItem.number ? 'completed' : ''}`}>
              <div className="step-circle">
                {step > stepItem.number ? (
                  <svg className="step-check" viewBox="0 0 20 20" fill="currentColor">
                    <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                  </svg>
                ) : (
                  <span className="step-number">{stepItem.number}</span>
                )}
              </div>
              <div className="step-content">
                <div className="step-title">{stepItem.title}</div>
                <div className="step-description">{stepItem.description}</div>
              </div>
            </div>
            {index < steps.length - 1 && (
              <div className={`stepper-line ${step > stepItem.number ? 'completed' : ''}`}></div>
            )}
          </div>
        ))}
      </div>
    </div>
  )

  return (
    <div className="login-container">
      <div className="login-card">
        <div className="login-header">
          <div className="login-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
              <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
              <circle cx="8.5" cy="7" r="4"/>
              <line x1="20" y1="8" x2="20" y2="14"/>
              <line x1="23" y1="11" x2="17" y2="11"/>
            </svg>
          </div>
          <h1>Create Account</h1>
          <p>Join the CDB ecosystem - Step {step} of 3</p>
        </div>
        {serverError && (
          <div className="alert error" role="alert">
            <svg className="alert-icon" viewBox="0 0 20 20" fill="currentColor">
              <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
            </svg>
            {serverError}
          </div>
        )}

        {step === 1 && (
          <div className="login-form">
              <div className="form-group">
                <label className="form-label">
                  Organization Name *
                  <input 
                    type="text" 
                    className={`form-input ${errors.name ? 'error' : ''}`}
                    value={name} 
                    onChange={(e) => setName(e.target.value)} 
                    placeholder="Enter your organization name"
                    required 
                  />
                  {errors.name && <span className="field-error">{errors.name}</span>}
                </label>
              </div>

              <div className="form-group">
                <label className="form-label">
                  Unique Code *
                  <input 
                    type="text" 
                    className={`form-input ${errors.code ? 'error' : ''}`}
                    value={code} 
                    onChange={(e) => setCode(e.target.value)} 
                    placeholder="e.g. acme-health-services"
                    required 
                  />
                  {errors.code && <span className="field-error">{errors.code}</span>}
                  <small className="form-hint">This will be your unique identifier in the system</small>
                </label>
              </div>



              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">
                    Provider Email Contact
                    <input 
                      type="email" 
                      className={`form-input ${errors.contactEmail ? 'error' : ''}`}
                      value={contactEmail} 
                      onChange={(e) => setContactEmail(e.target.value)}
                      placeholder="contact@yourorg.com"
                    />
                    {errors.contactEmail && <span className="field-error">{errors.contactEmail}</span>}
                  </label>
                </div>
                <div className="form-group">
                  <label className="form-label">
                    Provider Mobile Contact
                    <input 
                      type="tel" 
                      className="form-input"
                      value={contactPhone} 
                      onChange={(e) => setContactPhone(e.target.value)}
                      placeholder="+1 (555) 123-4567"
                    />
                  </label>
                </div>
              </div>


            <button type="button" className="login-btn" onClick={nextFromStep1}>
              Next
            </button>
          </div>
        )}

        {step === 2 && (
          <div className="login-form">


              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">
                    First Name
                    <input 
                      type="text" 
                      className="form-input"
                      value={adminFirstName} 
                      onChange={(e) => setAdminFirstName(e.target.value)}
                      placeholder="First name"
                    />
                  </label>
                </div>
                <div className="form-group">
                  <label className="form-label">
                    Last Name
                    <input 
                      type="text" 
                      className="form-input"
                      value={adminLastName} 
                      onChange={(e) => setAdminLastName(e.target.value)}
                      placeholder="Last name"
                    />
                  </label>
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">
                  Email (Login) *
                  <input 
                    type="email" 
                    className={`form-input ${errors.adminEmail ? 'error' : ''}`}
                    value={adminEmail} 
                    onChange={(e) => setAdminEmail(e.target.value)} 
                    placeholder="admin@yourorg.com"
                    required 
                  />
                  {errors.adminEmail && <span className="field-error">{errors.adminEmail}</span>}
                  <small className="form-hint">This email will be used for login</small>
                </label>
              </div>

              <div className="form-group">
                <label className="form-label">
                  Password *
                  <div className="input-wrapper">
                    <input 
                      type={showPassword ? 'text' : 'password'} 
                      className={`form-input ${errors.adminPassword ? 'error' : ''}`}
                      value={adminPassword} 
                      onChange={(e) => setAdminPassword(e.target.value)} 
                      placeholder="Create a secure password"
                      required 
                    />
                    <button 
                      type="button" 
                      className="password-toggle"
                      onClick={() => setShowPassword(!showPassword)}
                    >
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                        {showPassword ? (
                          <>
                            <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
                            <line x1="1" y1="1" x2="23" y2="23"/>
                          </>
                        ) : (
                          <>
                            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                            <circle cx="12" cy="12" r="3"/>
                          </>
                        )}
                      </svg>
                    </button>
                  </div>
                  {errors.adminPassword && <span className="field-error">{errors.adminPassword}</span>}
                  <small className="form-hint">Password must be at least 8 characters long</small>
                </label>
              </div>
            <div className="form-options">
              <button type="button" className="forgot-link" onClick={() => setStep(1)}>← Back</button>
            </div>
            <button type="button" className="login-btn" onClick={nextFromStep2}>
              Continue
            </button>
          </div>
        )}

        {step === 3 && (
          <div className="login-form">
            <h2>Verify Email</h2>
            <p>We'll send a verification code to <strong>{adminEmail || 'your email'}</strong></p>
              <div className="verification-section">
                <div className="verification-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                    <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                    <polyline points="22,6 12,13 2,6"/>
                  </svg>
                </div>
                
                <div className="form-group">
                  <button 
                    type="button" 
                    className={`btn ${otpSent ? 'btn-success' : 'btn-outline'}`}
                    onClick={sendOtp} 
                    disabled={otpSent}
                  >
                    {otpSent ? (
                      <>
                        <svg className="btn-icon-left" viewBox="0 0 20 20" fill="currentColor">
                          <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                        </svg>
                        OTP Sent
                      </>
                    ) : (
                      'Send Verification Code'
                    )}
                  </button>
                </div>

                <div className="form-group">
                  <label className="form-label">
                    Enter 6-digit verification code
                    <input 
                      type="text" 
                      className="form-input otp-input"
                      inputMode="numeric" 
                      pattern="\\d{6}" 
                      maxLength={6} 
                      value={otp} 
                      onChange={(e) => setOtp(e.target.value.replace(/[^0-9]/g, ''))}
                      placeholder="000000"
                    />
                  </label>
                </div>

                <div className="verification-note">
                  <svg className="info-icon" viewBox="0 0 20 20" fill="currentColor">
                    <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
                  </svg>
                  <small>For testing purposes, you can use <code>000000</code> as the verification code</small>
                </div>
              </div>
            <div className="form-options">
              <button type="button" className="forgot-link" onClick={() => setStep(2)}>← Back</button>
            </div>
            <button 
              type="button" 
              className="login-btn" 
              onClick={doVerifyOtp} 
              disabled={submitting}
            >
              {submitting ? (
                <>
                  <svg className="btn-spinner" viewBox="0 0 24 24">
                    <circle cx="12" cy="12" r="10" fill="none" stroke="currentColor" strokeWidth="4" opacity="0.25"/>
                    <path fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" opacity="0.75"/>
                  </svg>
                  Registering...
                </>
              ) : (
                'Complete Registration'
              )}
            </button>
          </div>
        )}
        
        <div className="login-footer">
          <p>Already have an account? <Link to="/login" className="signup-link">Sign in</Link></p>
        </div>
      </div>
    </div>
  )
}