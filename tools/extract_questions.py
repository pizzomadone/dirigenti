import json
import os
import re
import zlib

PDF = 'Domande_Prova-Preselettiva_DS.pdf'
OUT = 'app/src/main/assets/questions.json'
TOTAL_PAGES = '1453'

AREA_NAMES = {
    '1': 'Normativa del sistema educativo',
    '2': 'Conduzione delle organizzazioni complesse',
    '3': 'Programmazione, gestione e valutazione scolastica',
    '4': 'Ambienti di apprendimento, inclusione e digitale',
    '5': 'Organizzazione del lavoro e gestione del personale',
    '6': 'Valutazione e autovalutazione delle scuole',
    '7': 'Diritto civile e amministrativo',
    '8': 'Contabilità di Stato e gestione finanziaria',
    '9': 'Sistemi educativi europei',
}


def pdf_streams(data):
    for marker in re.finditer(rb'stream\r?\n', data):
        end = data.find(b'endstream', marker.end())
        if end < 0:
            continue
        raw = data[marker.end():end].strip(b'\r\n')
        try:
            yield zlib.decompress(raw)
        except Exception:
            continue


def build_cmap(data):
    cmap = {}
    for stream in pdf_streams(data):
        if b'beginbfrange' not in stream:
            continue
        for start, stop, first in re.findall(rb'<([0-9a-fA-F]{4})><([0-9a-fA-F]{4})><([0-9a-fA-F]{4})>', stream):
            start_i = int(start, 16)
            stop_i = int(stop, 16)
            first_i = int(first, 16)
            for code in range(start_i, stop_i + 1):
                cmap[code] = chr(first_i + code - start_i)
    return cmap


def decode_hex(hex_text, cmap):
    return ''.join(cmap.get(int(hex_text[i:i + 4], 16), '') for i in range(0, len(hex_text), 4))


def extract_text(data, cmap):
    pages = []
    for stream in pdf_streams(data):
        if b'Tj' not in stream and b'TJ' not in stream:
            continue
        chunks = []
        for match in re.finditer(rb'<([0-9A-Fa-f]+)>\s*Tj|\[((?:<[^>]+>|[-0-9 ]+)+)\]\s*TJ', stream):
            if match.group(1):
                chunks.append(decode_hex(match.group(1).decode(), cmap))
            else:
                chunks.append(''.join(decode_hex(part.decode(), cmap) for part in re.findall(rb'<([0-9A-Fa-f]+)>', match.group(2))))
        text = ' '.join(chunk.strip() for chunk in chunks if chunk.strip())
        if len(text) > 20:
            pages.append(text)
    return '\n'.join(pages)


def clean_text(text):
    text = re.sub(r'\s+', ' ', text).strip()
    text = re.sub(rf'\s*\b\d{{1,4}}/{TOTAL_PAGES}\b\s*', ' ', text)
    text = re.sub(r'\s+([,.;:])', r'\1', text)
    return re.sub(r'\s+', ' ', text).strip()


def make_explanation(question_text, correct_answer, rif, area_name):
    short_question = question_text.rstrip('?:. ')
    if len(short_question) > 180:
        short_question = short_question[:177].rstrip() + '...'
    return clean_text(
        f'In parole semplici: per questa domanda devi collegare il riferimento {rif} '
        f'({area_name}) alla risposta corretta: "{correct_answer}". '
        f'La domanda chiede: {short_question}. Tra le alternative, questa è quella da ricordare.'
    )


def parse_questions(full_text):
    marker = re.compile(r'(?:\[RIF\.\s*([0-9]+\.[0-9]+)\]\s*(?:\d{1,4}/' + TOTAL_PAGES + r'\s*)?(?:DOMANDA\s+([0-9.]+)\s+)?|\bDOMANDA\s+([0-9]+\.[0-9]+)\s+)', re.S)
    matches = list(marker.finditer(full_text))
    questions = []
    for idx, match in enumerate(matches):
        next_start = matches[idx + 1].start() if idx + 1 < len(matches) else len(full_text)
        block = full_text[match.end():next_start]
        block = clean_text(block)
        answers_match = re.search(r'\[a\](.*?)\[b\](.*?)\[c\](.*?)\[d\](.*)', block)
        if not answers_match:
            continue
        rif = (match.group(1) or match.group(2) or match.group(3)).strip()
        area_code = rif.split('.')[0]
        question_text = clean_text(block[:answers_match.start()])
        answers = [clean_text(answers_match.group(i)) for i in range(1, 5)]
        if not question_text or any(not answer for answer in answers):
            continue
        area_name = AREA_NAMES.get(area_code, f'Area {area_code}')
        questions.append({
            'id': rif,
            'rif': rif,
            'area': area_code,
            'areaName': area_name,
            'question': question_text,
            'answers': answers,
            'correct': 0,
            'explanation': make_explanation(question_text, answers[0], rif, area_name),
        })
    return questions


def main():
    data = open(PDF, 'rb').read()
    questions = parse_questions(extract_text(data, build_cmap(data)))
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, 'w', encoding='utf-8') as out:
        json.dump(questions, out, ensure_ascii=False, separators=(',', ':'))
    print(f'Wrote {len(questions)} questions to {OUT}')


if __name__ == '__main__':
    main()
