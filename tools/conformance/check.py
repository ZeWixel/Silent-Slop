#!/usr/bin/env python3
"""Link-check the compiled MIDlet against a real J2ME implementation.

The stubs in ``stubs/`` are compile-time only, and a mistake in them ships a
reference to a class that exists on no device (that is exactly how
``javax.microedition.lcdui.GameCanvas`` - instead of
``javax.microedition.lcdui.game.GameCanvas`` - ended up in the JAR and made the
game crash on start).  This script reads every class reference out of the
built classes and resolves it against the *source* of a real implementation.

Usage:
    python3 tools/conformance/check.py build/classes /path/to/j2me-loader/app/src/main/java
"""
import glob
import os
import struct
import sys


def constant_pool(d):
    n = struct.unpack('>H', d[8:10])[0]
    i, pool, k = 10, {}, 1
    while k < n:
        tag = d[i]
        i += 1
        if tag == 1:
            ln = struct.unpack('>H', d[i:i + 2])[0]
            i += 2
            pool[k] = ('utf8', d[i:i + ln].decode('utf8', 'replace'))
            i += ln
        elif tag in (7, 8, 16, 19, 20):
            pool[k] = (tag, struct.unpack('>H', d[i:i + 2])[0])
            i += 2
        elif tag == 15:
            pool[k] = (15, struct.unpack('>BH', d[i:i + 3]))
            i += 3
        elif tag in (3, 4):
            pool[k] = (tag, struct.unpack('>I', d[i:i + 4])[0])
            i += 4
        elif tag in (9, 10, 11, 12, 17, 18):
            pool[k] = (tag, struct.unpack('>HH', d[i:i + 4]))
            i += 4
        elif tag in (5, 6):
            pool[k] = (tag, 0)
            i += 8
            k += 1
        else:
            raise ValueError('unknown constant pool tag %d' % tag)
        k += 1
    return pool


def main(classes_dir, api_dir):
    classes, members = set(), set()
    for f in glob.glob(os.path.join(classes_dir, '**', '*.class'), recursive=True):
        pool = constant_pool(open(f, 'rb').read())
        for tag, v in pool.values():
            if tag == 7:
                classes.add(pool[v][1])
            elif tag in (9, 10):
                ci, nti = v
                cls = pool[pool[ci][1]][1]
                members.add((cls, pool[pool[nti][1][0]][1]))
    problems = []
    checked = 0
    for c in sorted(classes):
        if not c.startswith('javax/microedition'):
            continue
        checked += 1
        if not os.path.exists(os.path.join(api_dir, c + '.java')):
            problems.append('missing class   %s' % c)
    for c, m in sorted(members):
        if not c.startswith('javax/microedition'):
            continue
        src = os.path.join(api_dir, c + '.java')
        if not os.path.exists(src):
            continue
        body = open(src, encoding='utf8', errors='ignore').read()
        if m == '<init>':
            continue
        if not any(p in body for p in (m + '(', ' ' + m + ' ', ' ' + m + ';',
                                       ' ' + m + '=', m + ')')):
            problems.append('missing member  %s.%s' % (c, m))
    if problems:
        print('\n'.join(problems))
        print('%d problem(s)' % len(problems))
        return 1
    print('LINK OK: %d javax classes and %d members resolve against the real API'
          % (checked, len([1 for c, _ in members if c.startswith('javax/microedition')])))
    return 0


if __name__ == '__main__':
    if len(sys.argv) != 3:
        print(__doc__)
        raise SystemExit(2)
    raise SystemExit(main(sys.argv[1], sys.argv[2]))
