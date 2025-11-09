
import { BrowserRouter, Route, Routes } from 'react-router'
import './App.css'
import Client from './pages/client'
import ControllerFrontend from './pages/ControllerFrontend'
import WorkerFrontend from './pages/WorkerFrontend'
import WorkerFrontend2 from './pages/WorkerFrontend2'
import WorkerFrontend3 from './pages/WorkerFrontend3'
import WorkerFrontend4 from './pages/WorkerFrontend4'

function App() {
 

  return (
   <>
   <BrowserRouter>
   <Routes>
    <Route path='/' element={<div>Home Page</div>} />
    <Route path='/client' element={<Client />} />

    <Route path="/controller" element={ <ControllerFrontend/> } />

    <Route path="/worker" element={ <WorkerFrontend/> } />
      <Route path="/worker2" element={ <WorkerFrontend2/> } />
        <Route path="/worker3" element={ <WorkerFrontend3/> } />
     <Route path="/worker4" element={ <WorkerFrontend4/> } />

   </Routes>
   </BrowserRouter>
    </>
  )
}

export default App
