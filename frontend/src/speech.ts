// Pronúncia: usa o áudio gravado do dicionário quando existe; senão, a voz do próprio navegador.
export function speak(text: string, audioUrl?: string | null) {
  if (audioUrl) {
    new Audio(audioUrl).play().catch(() => speakWithBrowserVoice(text));
    return;
  }
  speakWithBrowserVoice(text);
}

function speakWithBrowserVoice(text: string) {
  if (!("speechSynthesis" in window)) return;
  window.speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(text);
  utterance.lang = "en-US";
  utterance.rate = 0.9;
  window.speechSynthesis.speak(utterance);
}
