export function TypingIndicator() {
  return (
    <div className="flex justify-start max-w-3xl mx-auto">
      <div className="bg-white border border-gray-200 rounded-2xl px-6 py-4 shadow-sm">
        <div className="flex gap-1.5">
          <div className="size-2.5 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
          <div className="size-2.5 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
          <div className="size-2.5 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
        </div>
      </div>
    </div>
  );
}
