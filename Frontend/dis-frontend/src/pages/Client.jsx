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

  // 🆕 for /v1/client/getVal
  const [workerKey, setWorkerKey] = useState("");
  const [workerId, setWorkerId] = useState("");
  const [workerHost, setWorkerHost] = useState("");
  const [workerPort, setWorkerPort] = useState("");
  const [workerResult, setWorkerResult] = useState(null);
  const [workerError, setWorkerError] = useState(null);
  const [workerLoading, setWorkerLoading] = useState(false);

  // -----------------------------------
  // PUT handler
  // -----------------------------------
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

  // -----------------------------------
  // GET handler (via controller)
  // -----------------------------------
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
        headers: { Accept: "application/json" },
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

  // -----------------------------------
  // 🆕 Direct Worker GET handler (/v1/client/getVal)
  // -----------------------------------
  async function handleWorkerGet(e) {
    e.preventDefault();
    setWorkerLoading(true);
    setWorkerError(null);
    setWorkerResult(null);

    if (!workerKey.trim() || !workerHost.trim() || !workerPort.trim() || !workerId.trim()) {
      setWorkerError("All fields are required (key, id, host, port)");
      setWorkerLoading(false);
      return;
    }

    try {
      const params = new URLSearchParams({
        key: workerKey.trim(),
        id: workerId.trim(),
        host: workerHost.trim(),
        port: workerPort.trim(),
      });

      const res = await fetch(`http://localhost:8080/v1/client/get/val?${params.toString()}`, {
        method: "GET",
        headers: { Accept: "application/json" },
      });

      if (!res.ok) {
        const text = await res.text();
        throw new Error(`HTTP ${res.status} — ${text}`);
      }

      const data = await res.json();
      setWorkerResult(data);
    } catch (err) {
      setWorkerError(err.message || "Unknown error");
    } finally {
      setWorkerLoading(false);
    }
  }

  // -----------------------------------
  // UI Rendering
  // -----------------------------------
  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-white py-10">
      <div className="max-w-5xl mx-auto px-4">
        <header className="mb-8">
          <h1 className="text-2xl font-extrabold text-slate-800">Distributed KV Client</h1>
          {/* <p className="mt-1 text-sm text-slate-500">
            Frontend for <code>/v1/client/put</code>, <code>/v1/client/get</code>, and{" "}
            <code>/v1/client/getVal</code>
          </p> */}
        </header>

        <div className="grid gap-6 md:grid-cols-2">
          {/* PUT card */}
          <section className="bg-white rounded-2xl shadow-md p-6">
            <h2 className="text-lg font-semibold text-slate-800">PUT (store)</h2>
            <form onSubmit={handlePut} className="mt-4 space-y-4">
              <input
                className="block w-full rounded-lg border border-slate-200 shadow-sm px-3 py-2"
                placeholder="Key"
                value={putKey}
                onChange={(e) => setPutKey(e.target.value)}
              />
              <textarea
                className="block w-full rounded-lg border border-slate-200 shadow-sm px-3 py-2"
                placeholder="Value"
                rows={3}
                value={putValue}
                onChange={(e) => setPutValue(e.target.value)}
              />
              <button
                type="submit"
                className="bg-indigo-600 text-white px-4 py-2 rounded-lg hover:bg-indigo-700"
                disabled={putLoading}
              >
                {putLoading ? "Saving..." : "Save"}
              </button>
              {putError && <p className="text-red-600 text-sm">Error: {putError}</p>}
              {putResult && (
                <pre className="bg-slate-50 p-3 rounded text-sm border">
                  {JSON.stringify(putResult, null, 2)}
                </pre>
              )}
            </form>
          </section>

          {/* GET card (controller) */}
          <section className="bg-white rounded-2xl shadow-md p-6">
            <h2 className="text-lg font-semibold text-slate-800">GET (via Controller)</h2>
            <form onSubmit={handleGet} className="mt-4 space-y-4">
              <input
                className="block w-full rounded-lg border border-slate-200 shadow-sm px-3 py-2"
                placeholder="Key"
                value={getKey}
                onChange={(e) => setGetKey(e.target.value)}
              />
              <button
                type="submit"
                className="bg-emerald-600 text-white px-4 py-2 rounded-lg hover:bg-emerald-700"
                disabled={getLoading}
              >
                {getLoading ? "Fetching..." : "Fetch"}
              </button>
              {getError && <p className="text-red-600 text-sm">Error: {getError}</p>}
              {getResult && (
                <pre className="bg-slate-50 p-3 rounded text-sm border">
                  {JSON.stringify(getResult, null, 2)}
                </pre>
              )}
            </form>
          </section>

          {/* 🆕 Direct GET from Worker */}
          <section className="bg-white rounded-2xl shadow-md p-6 md:col-span-2">
            <h2 className="text-lg font-semibold text-slate-800">
              GET from Worker Directly (using /v1/client/getVal)
            </h2>
            <form onSubmit={handleWorkerGet} className="mt-4 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-3">
              <input
                className="rounded-lg border border-slate-200 shadow-sm px-3 py-2"
                placeholder="Key"
                value={workerKey}
                onChange={(e) => setWorkerKey(e.target.value)}
              />
              <input
                className="rounded-lg border border-slate-200 shadow-sm px-3 py-2"
                placeholder="Worker ID"
                value={workerId}
                onChange={(e) => setWorkerId(e.target.value)}
              />
              <input
                className="rounded-lg border border-slate-200 shadow-sm px-3 py-2"
                placeholder="Host"
                value={workerHost}
                onChange={(e) => setWorkerHost(e.target.value)}
              />
              <input
                type="number"
                className="rounded-lg border border-slate-200 shadow-sm px-3 py-2"
                placeholder="Port"
                value={workerPort}
                onChange={(e) => setWorkerPort(e.target.value)}
              />

              <div className="col-span-full flex items-center gap-3 mt-2">
                <button
                  type="submit"
                  className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700"
                  disabled={workerLoading}
                >
                  {workerLoading ? "Fetching..." : "Fetch from Worker"}
                </button>
                <button
                  type="button"
                  className="px-3 py-2 border rounded-lg"
                  onClick={() => {
                    setWorkerKey("");
                    setWorkerHost("");
                    setWorkerPort("");
                    setWorkerId("");
                    setWorkerResult(null);
                    setWorkerError(null);
                  }}
                >
                  Reset
                </button>
              </div>
            </form>

            {workerError && <p className="text-red-600 text-sm mt-2">Error: {workerError}</p>}
            {workerResult && (
              <pre className="bg-slate-50 p-3 rounded text-sm border mt-3">
                {JSON.stringify(workerResult, null, 2)}
              </pre>
            )}
          </section>
        </div>
      </div>
    </div>
  );
}
