"""Current release packaging. Use --prepare before verification, then run without arguments to seal."""
import runpy
from pathlib import Path
runpy.run_path(str(Path(__file__).with_name('package-city.py')),run_name='__main__')
