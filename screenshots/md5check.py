import hashlib, os, sys
d = os.path.dirname(os.path.abspath(__file__))
for f in ("real.png", "mine.png"):
    p = os.path.join(d, f)
    if os.path.exists(p):
        print(f, hashlib.md5(open(p, 'rb').read()).hexdigest(), os.path.getsize(p))
