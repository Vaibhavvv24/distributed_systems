import React, { useState } from "react";

// WorkerFrontend.jsx
// Tailwind CSS only — no inline styles. Place this file in your React app (e.g. src/components/WorkerFrontend.jsx).
// Ensure Tailwind is configured. Set API base via REACT_APP_API_BASE (e.g. "http://localhost:8081").


export default function WorkerFrontend4() {
  const [status, setStatus] = useState(null);
  const [statusError, setStatusError] = useState(null);
  const [statusLoading, setStatusLoading] = useState(false);

  const [getKey, setGetKey] = useState("");
  const [getResult, setGetResult] = useState(null);
  const [getLoading, setGetLoading] = useState(false);
  const [getError, setGetError] = useState(null);

  const [putKey, setPutKey] = useState("");
  const [putValue, setPutValue] = useState("");
  const [putResult, setPutResult] = useState(null);
  const [putLoading, setPutLoading] = useState(false);
  const [putError, setPutError] = useState(null);

  const [repKey, setRepKey] = useState("");
  const [repRecord, setRepRecord] = useState("{\"value\":\"...\"}");
  const [repLoading, setRepLoading] = useState(false);
  const [repResult, setRepResult] = useState(null);
  const [repError, setRepError] = useState(null);

  const [healthStatus, setHealthStatus] = useState(null);
  const [healthLoading, setHealthLoading] = useState(false);
  const [healthError, setHealthError] = useState(null);

  async function fetchStatus() {
    setStatusLoading(true);
    setStatusError(null);
    setStatus(null);
    try {
      const res = await fetch(`http://localhost:8084/v1/worker/status`);
      if (!res.ok) throw new Error(`HTTP ${res.status} — ${await res.text()}`);
      const text = await res.text();
      setStatus(text);
    } catch (err) {
      setStatusError(err.message || "Unknown error");
    } finally {
      setStatusLoading(false);
    }
  }

  async function handleGet(e) {
    e?.preventDefault();
    setGetLoading(true);
    setGetError(null);
    setGetResult(null);
    if (!getKey.trim()) {
      setGetError("Key is required");
      setGetLoading(false);
      return;
    }
    try {
      const params = new URLSearchParams({ key: getKey.trim() });
      const res = await fetch(`http://localhost:8084/v1/worker/get?${params.toString()}`);
      if (!res.ok) throw new Error(`HTTP ${res.status} — ${await res.text()}`);
      const data = await res.json();
      setGetResult(data);
    } catch (err) {
      setGetError(err.message || "Unknown error");
    } finally {
      setGetLoading(false);
    }
  }

  async function handlePut(e) {
    e?.preventDefault();
    setPutLoading(true);
    setPutError(null);
    setPutResult(null);
    if (!putKey.trim()) {
      setPutError("Key is required");
      setPutLoading(false);
      return;
    }
    try {
      const res = await fetch(`http://localhost:8084/v1/worker/put`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ key: putKey.trim(), value: putValue }),
      });
      if (!res.ok) throw new Error(`HTTP ${res.status} — ${await res.text()}`);
      const data = await res.json();
      setPutResult(data);
    } catch (err) {
      setPutError(err.message || "Unknown error");
    } finally {
      setPutLoading(false);
    }
  }

  // async function handleReplicate(e) {
  //   e?.preventDefault();
  //   setRepLoading(true);
  //   setRepError(null);
  //   setRepResult(null);
  //   if (!repKey.trim()) {
  //     setRepError("Key is required");
  //     setRepLoading(false);
  //     return;
  //   }
  //   let parsed;
  //   try {
  //     parsed = JSON.parse(repRecord);
  //   } catch (err) {
  //     setRepError("replicate record must be valid JSON");
  //     setRepLoading(false);
  //     return;
  //   }

  //   try {
  //     const res = await fetch(`http://localhost:8084/v1/worker/replicate/${encodeURIComponent(repKey.trim())}`, {
  //       method: "PUT",
  //       headers: { "Content-Type": "application/json" },
  //       body: JSON.stringify(parsed),
  //     });
  //     if (!res.ok) throw new Error(`HTTP ${res.status} — ${await res.text()}`);
  //     setRepResult("OK");
  //   } catch (err) {
  //     setRepError(err.message || "Unknown error");
  //   } finally {
  //     setRepLoading(false);
  //   }
  // }

  // async function checkHealth() {
  //   setHealthLoading(true);
  //   setHealthError(null);
  //   setHealthStatus(null);
  //   try {
  //     const res = await fetch(`http://localhost:8084/v1/worker/health`);
  //     if (!res.ok) throw new Error(`HTTP ${res.status} — ${await res.text()}`);
  //     setHealthStatus("healthy");
  //   } catch (err) {
  //     setHealthError(err.message || "Unknown error");
  //   } finally {
  //     setHealthLoading(false);
  //   }
  // }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-white py-10">
      <div className="max-w-4xl mx-auto px-4">
        <header className="mb-8">
          <h1 className="text-2xl font-extrabold text-slate-800">Worker 4 Dashboard</h1>
          <p className="mt-1 text-sm text-slate-500">Interact with worker endpoints: status, get, put.</p>
        </header>

        <div className="grid gap-6 md:grid-cols-2">
          <section className="bg-white rounded-2xl shadow-md p-6">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold text-slate-800">Status</h2>
              <span className="text-xs text-slate-500">GET /v1/worker/status</span>
            </div>

            <div className="mt-4 flex items-center gap-3">
              <button onClick={fetchStatus} disabled={statusLoading} className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-sky-600 text-white font-medium shadow hover:bg-sky-700 disabled:opacity-60">{statusLoading ? "Checking..." : "Check Status"}</button>
              <button onClick={() => { setStatus(null); setStatusError(null); }} className="px-3 py-2 rounded-lg border border-slate-200">Clear</button>
              <div className="ml-auto text-sm text-slate-500">{status ? <span className="text-green-600">{status}</span> : statusError ? <span className="text-red-600">{statusError}</span> : <span className="text-slate-400">idle</span>}</div>
            </div>
          </section>
{/* 
          <section className="bg-white rounded-2xl shadow-md p-6">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold text-slate-800">Health</h2>
              <span className="text-xs text-slate-500">GET /v1/worker/health</span>
            </div>

            <div className="mt-4 flex items-center gap-3">
              <button onClick={checkHealth} disabled={healthLoading} className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-emerald-600 text-white font-medium shadow hover:bg-emerald-700 disabled:opacity-60">{healthLoading ? "Checking..." : "Ping Health"}</button>
              <button onClick={() => { setHealthStatus(null); setHealthError(null); }} className="px-3 py-2 rounded-lg border border-slate-200">Clear</button>
              <div className="ml-auto text-sm text-slate-500">{healthStatus ? <span className="text-green-600">{healthStatus}</span> : healthError ? <span className="text-red-600">{healthError}</span> : <span className="text-slate-400">idle</span>}</div>
            </div>
          </section> */}

          <section className="bg-white rounded-2xl shadow-md p-6">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold text-slate-800">GET Value</h2>
              <span className="text-xs text-slate-500">GET /v1/worker/get?key=...</span>
            </div>

            <form onSubmit={handleGet} className="mt-4 space-y-3">
              <div>
                <label className="block text-sm font-medium text-slate-700">Key</label>
                <input value={getKey} onChange={(e) => setGetKey(e.target.value)} className="mt-1 block w-full rounded-lg border border-slate-200 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-300" placeholder="example-key" />
              </div>

              <div className="flex items-center gap-3">
                <button disabled={getLoading} className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-indigo-600 text-white font-medium shadow hover:bg-indigo-700 disabled:opacity-60">{getLoading ? "Fetching..." : "Fetch"}</button>
                <button type="button" onClick={() => { setGetKey(""); setGetResult(null); setGetError(null); }} className="px-3 py-2 rounded-lg border border-slate-200">Reset</button>
                <div className="ml-auto text-sm text-slate-500">{getResult ? <span className="text-green-600">Found</span> : getError ? <span className="text-red-600">Error</span> : <span className="text-slate-400">idle</span>}</div>
              </div>

              {getError && <div className="text-sm text-red-600">Error: {getError}</div>}
              {getResult && <div className="mt-2 rounded-md bg-slate-50 p-3 border border-slate-100 text-sm"><div className="text-xs text-slate-500">Response</div><pre className="mt-1 whitespace-pre-wrap text-sm text-slate-700">{JSON.stringify(getResult, null, 2)}</pre></div>}
            </form>
          </section>

          <section className="bg-white rounded-2xl shadow-md p-6">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold text-slate-800">PUT Value</h2>
              <span className="text-xs text-slate-500">POST /v1/worker/put</span>
            </div>

            <form onSubmit={handlePut} className="mt-4 space-y-3">
              <div>
                <label className="block text-sm font-medium text-slate-700">Key</label>
                <input value={putKey} onChange={(e) => setPutKey(e.target.value)} className="mt-1 block w-full rounded-lg border border-slate-200 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-300" placeholder="example-key" />
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700">Value</label>
                <textarea value={putValue} onChange={(e) => setPutValue(e.target.value)} className="mt-1 block w-full rounded-lg border border-slate-200 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-300" rows={4} placeholder="any string or JSON" />
              </div>

              <div className="flex items-center gap-3">
                <button disabled={putLoading} className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-emerald-600 text-white font-medium shadow hover:bg-emerald-700 disabled:opacity-60">{putLoading ? "Saving..." : "Save"}</button>
                <button type="button" onClick={() => { setPutKey(""); setPutValue(""); setPutResult(null); setPutError(null); }} className="px-3 py-2 rounded-lg border border-slate-200">Reset</button>
                <div className="ml-auto text-sm text-slate-500">{putResult ? <span className="text-green-600">Saved</span> : putError ? <span className="text-red-600">Error</span> : <span className="text-slate-400">idle</span>}</div>
              </div>

              {putError && <div className="text-sm text-red-600">Error: {putError}</div>}
              {putResult && <div className="mt-2 rounded-md bg-slate-50 p-3 border border-slate-100 text-sm"><div className="text-xs text-slate-500">Response</div><pre className="mt-1 whitespace-pre-wrap text-sm text-slate-700">{JSON.stringify(putResult, null, 2)}</pre></div>}
            </form>
          </section>

         
        </div>

      
      </div>
    </div>
  );
}
