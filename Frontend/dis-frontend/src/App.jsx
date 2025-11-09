
import { BrowserRouter, Route, Routes } from 'react-router'
import './App.css'
import Client from './pages/client'
import ControllerFrontend from './pages/ControllerFrontend'
import WorkerFrontend from './pages/WorkerFrontend'

function App() {
 

  return (
   <>
   <BrowserRouter>
   <Routes>
    <Route path='/' element={<div>Home Page</div>} />
    <Route path='/client' element={<Client />} />

    <Route path="/controller" element={ <ControllerFrontend/> } />

    <Route path="/worker" element={ <WorkerFrontend/> } />


   </Routes>
   </BrowserRouter>
    </>
  )
}

export default App
