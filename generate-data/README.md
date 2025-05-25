# Generate Testing Data

Simple Python script to generate lots of data for testing.

## Prerequisites

* Python 3.12.1 or higher
* Homebrew (for MacOS users)

## Setup

1. Step into the directory:
   ```bash
   cd data-generator
   ```

2. Create and activate a Python virtual environment:
   ```bash
   python3 -m venv .venv
   source .venv/bin/activate
   ```

3. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```

## Running the Script

```bash
python generateData.py --host localhost --user yourUser --password yourPassword --database yourDatabase --port yourPort
```

### By default
```bash
python generateData.py
```

## Exit the virtual environment
```bash
deactivate
```
