
import { BrowserRouter, Route, Routes } from 'react-router'
import './App.css'
import Client from './pages/client'

function App() {
 

  return (
   <>
   <BrowserRouter>
   <Routes>
    <Route path='/' element={<div>Home Page</div>} />
    <Route path='/client' element={<Client />} />


   </Routes>
   </BrowserRouter>
    </>
  )
}

export default App
