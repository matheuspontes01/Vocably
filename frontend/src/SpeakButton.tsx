import { speak } from "./speech";

export function SpeakButton({ text }: { text: string }) {
  return (
    <button
      type="button"
      className="speak"
      title="Ouvir pronúncia"
      aria-label={`Ouvir pronúncia de ${text}`}
      onClick={(e) => {
        e.stopPropagation(); // não vira o flashcard ao clicar no alto-falante
        speak(text);
      }}
    >
      🔊
    </button>
  );
}
