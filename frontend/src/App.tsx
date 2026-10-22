import { useState } from 'react'
import heroImg from './assets/hero.png'
import reactLogo from './assets/react.svg'
import viteLogo from './assets/vite.svg'

function App() {
  const [count, setCount] = useState(0)

  return (
      <div className="flex items-center justify-center h-screen bg-blue-100">
        <h1 className="text-4xl font-bold text-blue-600 underline">
          Tailwind is working!
        </h1>
      </div>
  )
}

export default App
