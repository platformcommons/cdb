import React, { useState, useEffect, useRef } from 'react'

export default function AutocompleteInput({ 
  value, 
  onChange, 
  onSearch, 
  placeholder, 
  label,
  hint 
}) {
  const [inputValue, setInputValue] = useState('')
  const [suggestions, setSuggestions] = useState([])
  const [showSuggestions, setShowSuggestions] = useState(false)
  const [selectedIndex, setSelectedIndex] = useState(-1)
  const inputRef = useRef(null)
  const suggestionsRef = useRef(null)

  // Parse comma-separated values
  const values = value ? value.split(',').map(v => v.trim()).filter(Boolean) : []

  useEffect(() => {
    const searchTerm = inputValue.trim()
    if (searchTerm.length > 0) {
      onSearch(searchTerm).then(results => {
        // Filter out already selected values
        const filtered = results.filter(item => 
          !values.some(v => v.toLowerCase() === item.toLowerCase())
        )
        setSuggestions(filtered)
        setShowSuggestions(filtered.length > 0)
      }).catch(() => {
        setSuggestions([])
        setShowSuggestions(false)
      })
    } else {
      setSuggestions([])
      setShowSuggestions(false)
    }
  }, [inputValue, value, onSearch])

  const addValue = (newValue) => {
    if (!newValue.trim()) return
    
    const trimmedValue = newValue.trim()
    if (!values.some(v => v.toLowerCase() === trimmedValue.toLowerCase())) {
      const newValues = [...values, trimmedValue]
      onChange(newValues.join(', '))
    }
    setInputValue('')
    setShowSuggestions(false)
    setSelectedIndex(-1)
  }

  const removeValue = (indexToRemove) => {
    const newValues = values.filter((_, index) => index !== indexToRemove)
    onChange(newValues.join(', '))
  }

  const handleKeyDown = (e) => {
    if (e.key === 'Enter') {
      e.preventDefault()
      if (selectedIndex >= 0 && suggestions[selectedIndex]) {
        addValue(suggestions[selectedIndex])
      } else if (inputValue.trim()) {
        addValue(inputValue.trim())
      }
    } else if (e.key === 'ArrowDown') {
      e.preventDefault()
      setSelectedIndex(prev => Math.min(prev + 1, suggestions.length - 1))
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setSelectedIndex(prev => Math.max(prev - 1, -1))
    } else if (e.key === 'Escape') {
      setShowSuggestions(false)
      setSelectedIndex(-1)
    } else if (e.key === 'Backspace' && !inputValue && values.length > 0) {
      removeValue(values.length - 1)
    } else if (e.key === ',' || e.key === 'Tab') {
      e.preventDefault()
      if (inputValue.trim()) {
        addValue(inputValue.trim())
      }
    }
  }

  const handleSuggestionClick = (suggestion) => {
    addValue(suggestion)
    inputRef.current?.focus()
  }

  return (
    <div className="form-group">
      <label>{label}</label>
      <div className="autocomplete-container" style={{ position: 'relative' }}>
        <input
          ref={inputRef}
          type="text"
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          onKeyDown={handleKeyDown}
          onFocus={() => {
            if (suggestions.length > 0) setShowSuggestions(true)
          }}
          onBlur={() => {
            setTimeout(() => setShowSuggestions(false), 150)
          }}
          placeholder={placeholder}
          style={{
            width: '100%',
            padding: '12px 16px',
            border: '1px solid rgba(255,255,255,0.15)',
            borderRadius: '8px',
            background: 'rgba(255,255,255,0.04)',
            color: 'var(--text)',
            fontSize: '14px',
            outline: 'none'
          }}
        />
        
        {values.length > 0 && (
          <div style={{
            display: 'flex',
            flexWrap: 'wrap',
            gap: '6px',
            marginTop: '8px'
          }}>
            {values.map((val, index) => (
              <span key={index} className="tag-chip" style={{
                background: 'var(--primary)',
                color: 'white',
                padding: '4px 10px',
                borderRadius: '16px',
                fontSize: '12px',
                display: 'flex',
                alignItems: 'center',
                gap: '6px'
              }}>
                {val}
                <button
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation()
                    removeValue(index)
                  }}
                  style={{
                    background: 'rgba(255,255,255,0.2)',
                    border: 'none',
                    color: 'white',
                    cursor: 'pointer',
                    padding: '0',
                    width: '16px',
                    height: '16px',
                    borderRadius: '50%',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: '12px'
                  }}
                >
                  ×
                </button>
              </span>
            ))}
          </div>
        )}
        
        {showSuggestions && suggestions.length > 0 && (
          <div
            ref={suggestionsRef}
            className="suggestions-dropdown"
            style={{
              position: 'absolute',
              top: '100%',
              left: 0,
              right: 0,
              background: 'var(--card)',
              border: '1px solid var(--border)',
              borderRadius: '6px',
              boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
              zIndex: 1000,
              maxHeight: '200px',
              overflowY: 'auto'
            }}
          >
            {suggestions.map((suggestion, index) => (
              <div
                key={suggestion}
                className="suggestion-item"
                onClick={() => handleSuggestionClick(suggestion)}
                style={{
                  padding: '8px 12px',
                  cursor: 'pointer',
                  background: index === selectedIndex ? 'var(--hover)' : 'transparent',
                  borderBottom: index < suggestions.length - 1 ? '1px solid var(--border)' : 'none'
                }}
                onMouseEnter={() => setSelectedIndex(index)}
              >
                {suggestion}
              </div>
            ))}
          </div>
        )}
      </div>
      {hint && <small className="form-hint">{hint}</small>}
    </div>
  )
}