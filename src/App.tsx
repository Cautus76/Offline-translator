import { useState, useRef, useEffect, useCallback } from 'react';

// Declarations for browser Web Speech API
interface SpeechRecognitionErrorEvent extends Event {
  readonly error: string;
  readonly message?: string;
}

interface SpeechRecognitionEvent extends Event {
  readonly resultIndex: number;
  readonly results: SpeechRecognitionResultList;
}

interface SpeechRecognitionInstance extends EventTarget {
  continuous: boolean;
  interimResults: boolean;
  lang: string;
  maxAlternatives: number;
  start: () => void;
  stop: () => void;
  abort: () => void;
  onstart: (() => void) | null;
  onend: (() => void) | null;
  onerror: ((event: SpeechRecognitionErrorEvent) => void) | null;
  onresult: ((event: SpeechRecognitionEvent) => void) | null;
}

type SpeechRecognitionConstructor = new () => SpeechRecognitionInstance;

declare global {
  interface Window {
    SpeechRecognition?: SpeechRecognitionConstructor;
    webkitSpeechRecognition?: SpeechRecognitionConstructor;
  }
}

export default function App() {
  const [isListening, setIsListening] = useState<boolean>(false);
  const [committedText, setCommittedText] = useState<string>('');
  const [currentPartial, setCurrentPartial] = useState<string>('');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const isListeningRef = useRef<boolean>(false);
  const recognitionRef = useRef<SpeechRecognitionInstance | null>(null);
  const textareaRef = useRef<HTMLTextAreaElement | null>(null);

  // Map Web Speech error codes to specific code and meaning
  const getErrorMessage = (errorCode: string): string => {
    switch (errorCode) {
      case 'no-speech':
        return 'Chyba: no-speech – Žádná řeč nebyla detekována.';
      case 'audio-capture':
        return 'Chyba: audio-capture – Mikrofon není dostupný nebo nebyl nalezen.';
      case 'not-allowed':
        return 'Chyba: not-allowed – Přístup k mikrofonu byl zamítnut (vyžadováno oprávnění RECORD_AUDIO).';
      case 'network':
        return 'Chyba: network – Selhalo síťové spojení se službou rozpoznávání řeči.';
      case 'service-not-allowed':
        return 'Chyba: service-not-allowed – Služba rozpoznávání řeči není povolena.';
      case 'language-not-supported':
        return 'Chyba: language-not-supported – Jazyk de-DE není systémem podporován.';
      case 'aborted':
        return 'Chyba: aborted – Rozpoznávání řeči bylo přerušeno.';
      default:
        return `Chyba: ${errorCode} – Došlo k chybě rozpoznávání řeči.`;
    }
  };

  const startListening = useCallback(() => {
    setErrorMessage(null);

    const SpeechRecognitionClass =
      window.SpeechRecognition || window.webkitSpeechRecognition;

    if (!SpeechRecognitionClass) {
      setErrorMessage(
        'Chyba: speech-not-supported – Váš prohlížeč nebo zařízení nepodporuje SpeechRecognizer.'
      );
      return;
    }

    try {
      if (recognitionRef.current) {
        recognitionRef.current.abort();
      }

      const recognition = new SpeechRecognitionClass();
      recognition.lang = 'de-DE';
      recognition.continuous = true;
      recognition.interimResults = true;
      recognition.maxAlternatives = 1;

      recognition.onstart = () => {
        isListeningRef.current = true;
        setIsListening(true);
      };

      recognition.onresult = (event: SpeechRecognitionEvent) => {
        let finalResult = '';
        let partialResult = '';

        for (let i = 0; i < event.results.length; ++i) {
          const item = event.results[i];
          if (item && item[0]) {
            const transcript = item[0].transcript.trim();
            if (transcript) {
              if (item.isFinal) {
                finalResult = transcript;
              } else {
                partialResult = transcript;
              }
            }
          }
        }

        if (finalResult) {
          setCommittedText(finalResult);
          setCurrentPartial('');
        } else if (partialResult) {
          setCurrentPartial(partialResult);
        }
      };

      recognition.onerror = (event: SpeechRecognitionErrorEvent) => {
        if (event.error !== 'no-speech') {
          setErrorMessage(getErrorMessage(event.error));
        }
        isListeningRef.current = false;
        setIsListening(false);
      };

      recognition.onend = () => {
        isListeningRef.current = false;
        setIsListening(false);
        setCurrentPartial('');
      };

      recognitionRef.current = recognition;
      isListeningRef.current = true;
      setIsListening(true);
      recognition.start();
    } catch (err: unknown) {
      const errStr = err instanceof Error ? err.message : String(err);
      setErrorMessage(`Chyba: init-failed – ${errStr}`);
      isListeningRef.current = false;
      setIsListening(false);
    }
  }, []);

  const stopListening = useCallback(() => {
    isListeningRef.current = false;
    setIsListening(false);
    setCurrentPartial('');
    if (recognitionRef.current) {
      try {
        recognitionRef.current.stop();
      } catch {
        // Ignore stop error
      }
    }
  }, []);

  const handleToggle = () => {
    if (isListening) {
      stopListening();
    } else {
      setCommittedText('');
      setCurrentPartial('');
      startListening();
    }
  };

  // Auto-scroll textarea to bottom when text updates
  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.scrollTop = textareaRef.current.scrollHeight;
    }
  }, [committedText, currentPartial]);

  // Clean up on unmount
  useEffect(() => {
    return () => {
      isListeningRef.current = false;
      if (recognitionRef.current) {
        recognitionRef.current.abort();
      }
    };
  }, []);

  const displayText =
    committedText && currentPartial
      ? `${committedText} ${currentPartial}`
      : committedText || currentPartial;

  return (
    <div className="min-h-screen bg-white text-neutral-900 flex flex-col items-center justify-start px-4 py-10 font-sans">
      <div className="w-full max-w-xl flex flex-col items-center">
        <h1
          id="app-title"
          className="text-xl md:text-2xl font-bold tracking-wider uppercase mb-8 text-neutral-900 text-center"
        >
          DIKTOVÁNÍ – NĚMČINA
        </h1>

        <button
          id="btn-toggle-dictation"
          type="button"
          onClick={handleToggle}
          className={`w-44 py-3 px-8 text-base font-bold uppercase tracking-wider rounded transition-colors mb-8 cursor-pointer ${
            isListening
              ? 'bg-red-600 hover:bg-red-700 text-white'
              : 'bg-neutral-900 hover:bg-neutral-800 text-white'
          }`}
        >
          {isListening ? 'STOP' : 'START'}
        </button>

        <textarea
          id="recognized-text-area"
          ref={textareaRef}
          value={displayText}
          readOnly
          placeholder=""
          rows={12}
          className="w-full p-4 text-lg font-normal leading-relaxed text-neutral-900 bg-neutral-50 border border-neutral-300 rounded focus:outline-none focus:border-neutral-500 resize-none shadow-none"
        />

        {errorMessage && (
          <p
            id="error-message"
            className="w-full text-xs text-neutral-600 mt-3 text-center"
          >
            {errorMessage}
          </p>
        )}
      </div>
    </div>
  );
}
