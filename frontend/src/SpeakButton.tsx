import { speak } from "./speech";

export function SpeakButton({ text, audioUrl }: { text: string; audioUrl?: string | null }) {
  return (
    <button
      type="button"
      className="speak"
      title="Ouvir pronúncia"
      aria-label={`Ouvir pronúncia de ${text}`}
      onClick={(e) => {
        e.stopPropagation(); // não vira o flashcard ao clicar no alto-falante
        speak(text, audioUrl);
      }}
    >
      🔊
    </button>
  );
}
