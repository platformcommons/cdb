import React from 'react'
import EnvironmentConfiguration from './EnvironmentConfiguration'

export default function Sandbox() {
  // For demo, use a hardcoded providerId. Replace with actual providerId from context/store as needed.
  const providerId = 1;
  return (
    <section>
      <h1>Environment Configuration</h1>
      <EnvironmentConfiguration providerId={providerId} />
    </section>
  )
}
