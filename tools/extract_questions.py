import json, os, re, zlib
PDF='Domande_Prova-Preselettiva_DS.pdf'
OUT='app/src/main/assets/questions.json'
b=open(PDF,'rb').read()
cmap={}
for m in re.finditer(rb'stream\r?\n', b):
    data=b[m.end():b.find(b'endstream',m.end())].strip(b'\r\n')
    try: out=zlib.decompress(data)
    except Exception: continue
    if b'beginbfrange' in out:
        for a,bb,c in re.findall(rb'<([0-9a-fA-F]{4})><([0-9a-fA-F]{4})><([0-9a-fA-F]{4})>', out):
            ai=int(a,16); bi=int(bb,16); ci=int(c,16)
            for x in range(ai,bi+1): cmap[x]=chr(ci+x-ai)
def dec(hexs):
    return ''.join(cmap.get(int(hexs[i:i+4],16),'') for i in range(0,len(hexs),4))
pages=[]
for m in re.finditer(rb'stream\r?\n', b):
    data=b[m.end():b.find(b'endstream',m.end())].strip(b'\r\n')
    try: out=zlib.decompress(data)
    except Exception: continue
    if b'Tj' not in out and b'TJ' not in out: continue
    txt=[]
    for hm in re.finditer(rb'<([0-9A-Fa-f]+)>\s*Tj|\[((?:<[^>]+>|[-0-9 ]+)+)\]\s*TJ', out):
        if hm.group(1): txt.append(dec(hm.group(1).decode()))
        else: txt.append(''.join(dec(p.decode()) for p in re.findall(rb'<([0-9A-Fa-f]+)>', hm.group(2))))
    text=' '.join(t.strip() for t in txt if t.strip())
    if len(text)>20: pages.append(text)
full='\n'.join(pages)
pat=re.compile(r'\[RIF\.\s*([^\]]+)\]\s*DOMANDA\s+([0-9.]+)\s+(.*?)(?=\[RIF\.\s*[^\]]+\]\s*DOMANDA\s+[0-9.]|\Z)', re.S)
questions=[]
for m in pat.finditer(full):
    block=' '.join(m.group(3).split())
    am=re.search(r'\[a\](.*?)\[b\](.*?)\[c\](.*?)\[d\](.*)', block)
    if not am: continue
    questions.append({'id':m.group(2),'rif':m.group(1),'area':m.group(1).split('.')[0],'question':block[:am.start()].strip(),'answers':[am.group(i).strip() for i in range(1,5)],'correct':0})
os.makedirs(os.path.dirname(OUT), exist_ok=True)
open(OUT,'w',encoding='utf-8').write(json.dumps(questions,ensure_ascii=False,separators=(',',':')))
print(f'Wrote {len(questions)} questions to {OUT}')
