"""Read numeric usage records from this task only. Never export conversation text."""
from pathlib import Path
from datetime import datetime, timezone
import argparse
import json

ROOT = Path(__file__).resolve().parents[1]
EVIDENCE = ROOT / 'evidence/caveman-pilot'
SESSION = Path(r'C:\Users\scarl\.codex\sessions\2026\09\08\rollout-2026-09-08T21-55-16-01a08116-4a40-70d2-bd3d-05911f17d34d.jsonl')

def usage_records():
    rows = []
    with SESSION.open('rb') as stream:
        stream.seek(max(0, SESSION.stat().st_size - 8_000_000))
        stream.readline()
        for line in stream:
            try:
                row = json.loads(line)
            except (ValueError, UnicodeDecodeError):
                continue
            if row.get('type') == 'token_usage_record':
                payload = row['payload']
                rows.append({'timestamp': row['timestamp'], 'ordinal': row['ordinal'],
                    'response_id': payload['response_id'], 'usage': payload['usage']})
    return rows

parser = argparse.ArgumentParser()
parser.add_argument('action', choices=['start', 'end', 'report'])
parser.add_argument('arm', nargs='?')
args = parser.parse_args()
if args.action != 'report':
    if args.arm not in ('A', 'B'):
        parser.error('arm must be A or B')
    rows = usage_records()
    marker = {'timestamp': datetime.now(timezone.utc).isoformat(), 'last_usage': rows[-1] if rows else None}
    target = EVIDENCE / f'{args.arm}-{args.action}.json'
    if target.exists():
        raise SystemExit('MEASUREMENT_MARKER_EXISTS: preserve previous run')
    target.write_text(json.dumps(marker, indent=2), encoding='utf-8')
    print(f'MEASUREMENT {args.arm} {args.action} {marker["timestamp"]}')
else:
    records = usage_records()
    result = {}
    for arm in ('A', 'B'):
        start = json.loads((EVIDENCE / f'{arm}-start.json').read_text())
        end = json.loads((EVIDENCE / f'{arm}-end.json').read_text())
        low, high = (datetime.fromisoformat(marker['timestamp']) for marker in (start, end))
        selected = [row for row in records if low <= datetime.fromisoformat(row['timestamp']) < high]
        if not selected or len({row['response_id'] for row in selected}) != len(selected):
            raise SystemExit('USAGE_INTERVAL_INVALID')
        totals = {key: sum(row['usage'].get(key, 0) for row in selected) for key in selected[0]['usage']}
        assert totals['total_tokens'] == totals['input_tokens'] + totals['output_tokens']
        totals['non_reasoning_output_tokens'] = totals['output_tokens'] - totals['reasoning_output_tokens']
        totals['uncached_input_tokens'] = totals['input_tokens'] - totals['cached_input_tokens']
        result[arm] = {'seconds': (high-low).total_seconds(), 'response_count': len(selected), 'tokens': totals, 'records': selected}
    result['observed_reduction_percent'] = {key: 100*(result['A']['tokens'][key]-result['B']['tokens'][key])/result['A']['tokens'][key]
        for key in result['A']['tokens'] if result['A']['tokens'][key]}
    result['interpretation'] = 'Observed sequential intervals, not causal Caveman savings, billing, quota or proof of quality equivalence.'
    (EVIDENCE/'usage.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
    print(json.dumps({arm:{k:v for k,v in result[arm].items() if k!='records'} for arm in ('A','B')},indent=2))
    print(json.dumps(result['observed_reduction_percent'],indent=2))
