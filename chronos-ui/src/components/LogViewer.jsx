export default function LogViewer({ content, onClose }) {
  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center">
      <div className="bg-white rounded-lg shadow-lg p-6 w-3/4 max-h-[80vh] overflow-y-auto">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-xl font-bold">Job Log Output</h2>
          <button onClick={onClose} className="text-red-500 hover:text-red-700">
            Close ✕
          </button>
        </div>
        <pre className="bg-gray-100 p-3 rounded text-sm overflow-x-auto">{content}</pre>
      </div>
    </div>
  );
}
