import React, { useState } from "react";


export default function ControllerFrontend() {
  
  const [regId, setRegId] = useState("");
  const [regHost, setRegHost] = useState("");
  const [regPort, setRegPort] = useState("");
  const [regLoading, setRegLoading] = useState(false);
  const [regResult, setRegResult] = useState(null);
  const [regError, setRegError] = useState(null);

  const [hbId, setHbId] = useState("");
  const [hbLoading, setHbLoading] = useState(false);
  const [hbResult, setHbResult] = useState(null);
  const [hbError, setHbError] = useState(null);

  
  const [mapKey, setMapKey] = useState("");
  const [mapLoading, setMapLoading] = useState(false);
  const [mapResult, setMapResult] = useState(null);
  const [mapError, setMapError] = useState(null);

  
  const [workersLoading, setWorkersLoading] = useState(false);
  const [workers, setWorkers] = useState(null);
  const [workersError, setWorkersError] = useState(null);

  
  const [rerepLoading, setRerepLoading] = useState(false);
  const [rerepResult, setRerepResult] = useState(null);
  const [rerepError, setRerepError] = useState(null);

  async function handleRegister(e) {
    e?.preventDefault();
    setRegLoading(true);
    setRegError(null);
    setRegResult(null);

    if (!regId.trim() || !regHost.trim() || !regPort) {
      setRegError("id, host and port are required");
      setRegLoading(false);
      return;
    }

    try {
  const formData = new URLSearchParams();
  formData.append("id", regId.trim());
  formData.append("host", regHost.trim());
  formData.append("port", String(regPort));

  const res = await fetch("http://localhost:8085/v1/controller/register", {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body: formData.toString(),
  });

  if (!res.ok) throw new Error(`HTTP ${res.status} — ${await res.text()}`);

  const text = await res.text();
  setRegResult(text);
} catch (err) {
  setRegError(err.message || "Unknown error");
} finally {
  setRegLoading(false);
}

  }

  async function handleHeartbeat(e) {
    e?.preventDefault();
    setHbLoading(true);
    setHbError(null);
    setHbResult(null);

    if (!hbId.trim()) {
      setHbError("id is required");
      setHbLoading(false);
      return;
    }

    try {
      const res = await fetch(`http://localhost:8085/v1/controller/heartbeat`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ id: hbId.trim() }),
      });
      if (!res.ok) throw new Error(`HTTP ${res.status} — ${await res.text()}`);
      const data = await res.json();
      setHbResult(data);
    } catch (err) {
      setHbError(err.message || "Unknown error");
    } finally {
      setHbLoading(false);
    }
  }

  async function handleGetKeyMapping(e) {
    e?.preventDefault();
    setMapLoading(true);
    setMapError(null);
    setMapResult(null);

    if (!mapKey.trim()) {
      setMapError("key is required");
      setMapLoading(false);
      return;
    }

    try {
      const res = await fetch(`http://localhost:8085/v1/controller/key-mapping/${encodeURIComponent(mapKey.trim())}`);
      if (!res.ok) throw new Error(`HTTP ${res.status} — ${await res.text()}`);
      const data = await res.json();
      setMapResult(data);
    } catch (err) {
      setMapError(err.message || "Unknown error");
    } finally {
      setMapLoading(false);
    }
  }

  async function handleListWorkers() {
    setWorkersLoading(true);
    setWorkersError(null);
    setWorkers(null);
    try {
      const res = await fetch(`http://localhost:8085/v1/controller/workers`);
      if (!res.ok) throw new Error(`HTTP ${res.status} — ${await res.text()}`);
      const data = await res.json();
      setWorkers(data);
    } catch (err) {
      setWorkersError(err.message || "Unknown error");
    } finally {
      setWorkersLoading(false);
    }
  }

  async function handleTriggerReReplication() {
    setRerepLoading(true);
    setRerepError(null);
    setRerepResult(null);
    try {
      const res = await fetch(`http://localhost:8085/v1/controller/trigger-rereplicate`, { method: "POST" });
      if (!res.ok) throw new Error(`HTTP ${res.status} — ${await res.text()}`);
      setRerepResult("Triggered");
    } catch (err) {
      setRerepError(err.message || "Unknown error");
    } finally {
      setRerepLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-white py-10">
      <div className="max-w-6xl mx-auto px-4">
        <header className="mb-8">
          <h1 className="text-2xl font-extrabold text-slate-800">Controller Dashboard</h1>
          <p className="mt-1 text-sm text-slate-500">Interact with controller endpoints (register, key mapping, workers)</p>
        </header>

        <div className="grid gap-6 md:grid-cols-2">
          {/* Register worker */}
          <section className="bg-white rounded-2xl shadow-md p-6">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold text-slate-800">Register Worker</h2>
              <span className="text-xs text-slate-500">POST /v1/controller/register?id=&host=&port=</span>
            </div>

            <form onSubmit={handleRegister} className="mt-4 space-y-3">
              <div>
                <label className="block text-sm font-medium text-slate-700">Worker ID</label>
                <input value={regId} onChange={(e) => setRegId(e.target.value)} className="mt-1 block w-full rounded-lg border border-slate-200 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-300" placeholder="worker-1" />
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700">Host</label>
                <input value={regHost} onChange={(e) => setRegHost(e.target.value)} className="mt-1 block w-full rounded-lg border border-slate-200 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-300" placeholder="127.0.0.1" />
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700">Port</label>
                <input value={regPort} onChange={(e) => setRegPort(e.target.value)} className="mt-1 block w-full rounded-lg border border-slate-200 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-300" placeholder="8081" type="number" />
              </div>

              <div className="flex items-center gap-3">
                <button disabled={regLoading} className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-indigo-600 text-white font-medium shadow hover:bg-indigo-700 disabled:opacity-60">{regLoading ? "Registering..." : "Register"}</button>
                <button type="button" onClick={() => { setRegId(""); setRegHost(""); setRegPort(""); setRegResult(null); setRegError(null); }} className="px-3 py-2 rounded-lg border border-slate-200">Reset</button>
                <div className="ml-auto text-sm text-slate-500">{regResult ? <span className="text-green-600">OK</span> : regError ? <span className="text-red-600">Error</span> : <span className="text-slate-400">idle</span>}</div>
              </div>

              {regError && <div className="text-sm text-red-600">Error: {regError}</div>}
              {regResult && <div className="mt-2 text-sm text-slate-700">{regResult}</div>}
            </form>
          </section>

          {/* Heartbeat
          <section className="bg-white rounded-2xl shadow-md p-6">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold text-slate-800">Heartbeat</h2>
              <span className="text-xs text-slate-500">POST /v1/controller/heartbeat</span>
            </div>

            <form onSubmit={handleHeartbeat} className="mt-4 space-y-3">
              <div>
                <label className="block text-sm font-medium text-slate-700">Worker ID</label>
                <input value={hbId} onChange={(e) => setHbId(e.target.value)} className="mt-1 block w-full rounded-lg border border-slate-200 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-emerald-300" placeholder="worker-1" />
              </div>

              <div className="flex items-center gap-3">
                <button disabled={hbLoading} className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-emerald-600 text-white font-medium shadow hover:bg-emerald-700 disabled:opacity-60">{hbLoading ? "Sending..." : "Send"}</button>
                <button type="button" onClick={() => { setHbId(""); setHbResult(null); setHbError(null); }} className="px-3 py-2 rounded-lg border border-slate-200">Reset</button>
                <div className="ml-auto text-sm text-slate-500">{hbResult ? <span className="text-green-600">OK</span> : hbError ? <span className="text-red-600">Error</span> : <span className="text-slate-400">idle</span>}</div>
              </div>

              {hbError && <div className="text-sm text-red-600">Error: {hbError}</div>}
              {hbResult && <div className="mt-2 rounded-md bg-slate-50 p-3 border border-slate-100 text-sm"><div className="text-xs text-slate-500">Response</div><pre className="mt-1 whitespace-pre-wrap text-sm text-slate-700">{JSON.stringify(hbResult, null, 2)}</pre></div>}
            </form>
          </section> */}

          {/* Key mapping */}
          <section className="bg-white rounded-2xl shadow-md p-6">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold text-slate-800">Key Mapping</h2>
              <span className="text-xs text-slate-500">GET /v1/controller/key-mapping/{"{key}"}</span>
            </div>

            <form onSubmit={handleGetKeyMapping} className="mt-4 space-y-3">
              <div>
                <label className="block text-sm font-medium text-slate-700">Key</label>
                <input value={mapKey} onChange={(e) => setMapKey(e.target.value)} className="mt-1 block w-full rounded-lg border border-slate-200 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-300" placeholder="user:123" />
              </div>

              <div className="flex items-center gap-3">
                <button disabled={mapLoading} className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-indigo-600 text-white font-medium shadow hover:bg-indigo-700 disabled:opacity-60">{mapLoading ? "Fetching..." : "Lookup"}</button>
                <button type="button" onClick={() => { setMapKey(""); setMapResult(null); setMapError(null); }} className="px-3 py-2 rounded-lg border border-slate-200">Reset</button>
                <div className="ml-auto text-sm text-slate-500">{mapResult ? <span className="text-green-600">OK</span> : mapError ? <span className="text-red-600">Error</span> : <span className="text-slate-400">idle</span>}</div>
              </div>

              {mapError && <div className="text-sm text-red-600">Error: {mapError}</div>}
              {mapResult && <div className="mt-2 rounded-md bg-slate-50 p-3 border border-slate-100 text-sm"><div className="text-xs text-slate-500">Response</div><pre className="mt-1 whitespace-pre-wrap text-sm text-slate-700">{JSON.stringify(mapResult, null, 2)}</pre></div>}
            </form>
          </section>

          {/* Workers & Rereplication */}
          <section className="bg-white rounded-2xl shadow-md p-6 md:col-span-2">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold text-slate-800">Workers</h2>
              <span className="text-xs text-slate-500">GET /workers</span>
            </div>

            <div className="mt-4 space-y-4">
              <div className="flex items-center gap-3">
                <button onClick={handleListWorkers} disabled={workersLoading} className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-sky-600 text-white font-medium shadow hover:bg-sky-700 disabled:opacity-60">{workersLoading ? "Loading..." : "List Workers"}</button>
                <button onClick={() => { setWorkers(null); setWorkersError(null); }} className="px-3 py-2 rounded-lg border border-slate-200">Clear</button>

                {/* <div className="ml-auto flex items-center gap-3">
                  <button onClick={handleTriggerReReplication} disabled={rerepLoading} className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-rose-600 text-white font-medium shadow hover:bg-rose-700 disabled:opacity-60">{rerepLoading ? "Triggering..." : "Trigger Re-replication"}</button>
                  {rerepResult && <div className="text-sm text-green-600">{rerepResult}</div>}
                  {rerepError && <div className="text-sm text-red-600">{rerepError}</div>}
                </div> */}
              </div>

              {workersError && <div className="text-sm text-red-600">Error: {workersError}</div>}

              {workers && (
                <div className="mt-2 rounded-md bg-slate-50 p-3 border border-slate-100">
                  <div className="text-xs text-slate-500 mb-2">Workers ({workers.length})</div>
                  <div className="grid gap-2">
                    {workers.map((w, idx) => (
                      <div key={idx} className="flex items-center justify-between bg-white rounded-lg p-3 border">
                        <div>
                          <div className="text-sm font-medium text-slate-800">{w.id}</div>
                          <div className="text-xs text-slate-500">{w.host}:{w.port}</div>
                        </div>
                        <div className="text-xs text-slate-500">status: {w.alive ? 'alive' : 'dead'}</div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </section>
        </div>

      
      </div>
    </div>
  );
}
