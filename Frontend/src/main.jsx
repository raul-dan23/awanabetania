import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import { installAuthFetch, clearToken } from './auth.js'

// Attach the bearer token to every API call, and bounce to login if it is rejected.
installAuthFetch({
  onUnauthorized: () => {
    clearToken()
    localStorage.removeItem('awanaLoggedUser')
    localStorage.removeItem('awanaCurrentPage')
    window.location.reload()
  },
})

/** Application entry point — mounts the React tree into the #root DOM element. */
createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
