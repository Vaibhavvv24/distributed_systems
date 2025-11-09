import React, { useState } from "react";


export default function Client() {
  const [putKey, setPutKey] = useState("");
  const [putValue, setPutValue] = useState("");
  const [putResult, setPutResult] = useState(null);
  const [putLoading, setPutLoading] = useState(false);
  const [putError, setPutError] = useState(null);

  const [getKey, setGetKey] = useState("");
  const [getResult, setGetResult] = useState(null);
  const [getLoading, setGetLoading] = useState(false);
  const [getError, setGetError] = useState(null);

  async function handlePut(e) {
    e.preventDefault();
    setPutLoading(true);
    setPutError(null);
    setPutResult(null);

    if (!putKey.trim()) {
      setPutError("Key is required");
      setPutLoading(false);
      return;
    }

    try {
      const res = await fetch(`http://localhost:8080/v1/client/put`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ key: putKey.trim(), value: putValue }),
      });

      if (!res.ok) {
        const text = await res.text();
        throw new Error(`HTTP ${res.status} — ${text}`);
      }

      const data = await res.json();
      setPutResult(data);
    } catch (err) {
      setPutError(err.message || "Unknown error");
    } finally {
      setPutLoading(false);
    }
  }

  async function handleGet(e) {
    e.preventDefault();
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
      const res = await fetch(`http://localhost:8080/v1/client/get?${params.toString()}`, {
        method: "GET",
        headers: { "Accept": "application/json" },
      });

      if (!res.ok) {
        const text = await res.text();
        throw new Error(`HTTP ${res.status} — ${text}`);
      }

      const data = await res.json();
      setGetResult(data);
    } catch (err) {
      setGetError(err.message || "Unknown error");
    } finally {
      setGetLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-white py-10">
      <div className="max-w-5xl mx-auto px-4">
        <header className="mb-8">
          <h1 className="text-2xl font-extrabold text-slate-800">Distributed KV Client</h1>
          <p className="mt-1 text-sm text-slate-500">Simple frontend for <code className="bg-slate-100 px-1 rounded">/v1/client/put</code> and <code className="bg-slate-100 px-1 rounded">/v1/client/get</code></p>
        </header>

        <div className="grid gap-6 md:grid-cols-2">
          {/* PUT card */}
          <section className="bg-white rounded-2xl shadow-md p-6">
            <div className="flex items-start justify-between">
              <h2 className="text-lg font-semibold text-slate-800">PUT (store)</h2>
              <span className="text-xs text-slate-500">POST /v1/client/put</span>
            </div>

            <form onSubmit={handlePut} className="mt-4 space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700">Key</label>
                <input
                  className="mt-1 block w-full rounded-lg border border-slate-200 shadow-sm px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-300"
                  value={putKey}
                  onChange={(e) => setPutKey(e.target.value)}
                  placeholder="example-key"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700">Value</label>
                <textarea
                  className="mt-1 block w-full rounded-lg border border-slate-200 shadow-sm px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-300"
                  value={putValue}
                  onChange={(e) => setPutValue(e.target.value)}
                  placeholder='Any string or JSON, e.g. "{\"name\":\"A\"}"'
                  rows={4}
                />
              </div>

              <div className="flex items-center gap-3">
                <button
                  type="submit"
                  className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-indigo-600 text-white font-medium shadow hover:bg-indigo-700 disabled:opacity-60"
                  disabled={putLoading}
                >
                  {putLoading ? "Saving..." : "Save"}
                </button>

                <button
                  type="button"
                  className="px-3 py-2 rounded-lg border border-slate-200 text-sm"
                  onClick={() => {
                    setPutKey("");
                    setPutValue("");
                    setPutResult(null);
                    setPutError(null);
                  }}
                >
                  Reset
                </button>

                <div className="ml-auto text-sm text-slate-500">Status: {putResult ? <span className="text-green-600">Saved</span> : putError ? <span className="text-red-600">Error</span> : <span className="text-slate-400">idle</span>}</div>
              </div>

              {putError && <div className="text-sm text-red-600">Error: {putError}</div>}

              {putResult && (
                <div className="mt-2 rounded-md bg-slate-50 p-3 border border-slate-100 text-sm">
                  <div className="text-xs text-slate-500">Response</div>
                  <pre className="mt-1 whitespace-pre-wrap text-sm text-slate-700">{JSON.stringify(putResult, null, 2)}</pre>
                </div>
              )}
            </form>
          </section>

          {/* GET card */}
          <section className="bg-white rounded-2xl shadow-md p-6">
            <div className="flex items-start justify-between">
              <h2 className="text-lg font-semibold text-slate-800">GET (fetch)</h2>
              <span className="text-xs text-slate-500">GET /v1/client/get?key=...</span>
            </div>

            <form onSubmit={handleGet} className="mt-4 space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700">Key</label>
                <input
                  className="mt-1 block w-full rounded-lg border border-slate-200 shadow-sm px-3 py-2 focus:outline-none focus:ring-2 focus:ring-emerald-300"
                  value={getKey}
                  onChange={(e) => setGetKey(e.target.value)}
                  placeholder="example-key"
                />
              </div>

              <div className="flex items-center gap-3">
                <button
                  type="submit"
                  className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-emerald-600 text-white font-medium shadow hover:bg-emerald-700 disabled:opacity-60"
                  disabled={getLoading}
                >
                  {getLoading ? "Fetching..." : "Fetch"}
                </button>

                <button
                  type="button"
                  className="px-3 py-2 rounded-lg border border-slate-200 text-sm"
                  onClick={() => {
                    setGetKey("");
                    setGetResult(null);
                    setGetError(null);
                  }}
                >
                  Reset
                </button>

                <div className="ml-auto text-sm text-slate-500">Status: {getResult ? <span className="text-green-600">Found</span> : getError ? <span className="text-red-600">Error</span> : <span className="text-slate-400">idle</span>}</div>
              </div>

              {getError && <div className="text-sm text-red-600">Error: {getError}</div>}

              {getResult && (
                <div className="mt-2 rounded-md bg-slate-50 p-3 border border-slate-100 text-sm">
                  <div className="text-xs text-slate-500">Response</div>
                  <pre className="mt-1 whitespace-pre-wrap text-sm text-slate-700">{JSON.stringify(getResult, null, 2)}</pre>
                </div>
              )}
            </form>
          </section>
        </div>

        {/* <footer className="mt-6 text-sm text-slate-500">
          <ul className="list-disc ml-5 space-y-1">
            <li>Set <code className="bg-slate-100 px-1 rounded">REACT_APP_API_BASE</code> to your Spring Boot base URL if different from the frontend origin.</li>
            <li>Backend endpoints used: <code className="bg-slate-100 px-1 rounded">/v1/client/put</code> (POST JSON: <code className="bg-slate-100 px-1 rounded">{"key":"...","value":"..."}</code>) and <code className="bg-slate-100 px-1 rounded">/v1/client/get?key=...</code>.</li>
          </ul>
        </footer> */}
      </div>
    </div>
  );
}