import openpyxl
wb = openpyxl.load_workbook('C:/Users/laptop/Downloads/Satelites.xlsx')
ws = wb.active

print("=== EXACT CELL VALUES ROW 6 (HEADER) ===")
for cell in ws[6]:
    if cell.value:
        col = cell.column_letter
        print(f"  {col}{cell.row}: '{cell.value}'")

print("\n=== DATA ROW 7 (first user) ===")
for cell in ws[7]:
    if cell.value:
        col = cell.column_letter
        print(f"  {col}{cell.row}: '{cell.value}'")

print("\n=== COLUMN WIDTHS ===")
for letter in 'ABCDEFGHIJKLMNOPQRSTUVWXYZ':
    try:
        w = ws.column_dimensions[letter].width
        if w and w > 0: print(f"  {letter}: {w}")
    except: pass

# Check two-letter columns
for l1 in 'A':
    for l2 in 'ABCDEFGHIJ':
        letter = l1+l2
        try:
            w = ws.column_dimensions[letter].width
            if w and w > 0: print(f"  {letter}: {w}")
        except: pass

print("\n=== ROW HEIGHTS ===")
for r in range(1, 8):
    try:
        h = ws.row_dimensions[r].height
        if h: print(f"  Row {r}: {h}")
    except: pass

print("\n=== CELL FILLS/COLORS ROW 6 ===")
for cell in ws[6]:
    if cell.value:
        fc = None
        try:
            fc = cell.fill.start_color.rgb if cell.fill.patternType else None
        except: pass
        print(f"  {cell.column_letter}: fill={fc} bg={cell.fill.patternType}")

print("\n=== FIRST 3 DATA ROWS ATTENDANCE COLUMNS (H-AO) ===")
for r in [7, 8, 9]:
    row_data = []
    for cell in ws[r]:
        if cell.column >= 8 and cell.column <= 37:
            row_data.append(f"{cell.column_letter}={cell.value}")
    if row_data:
        name = ws.cell(r, 3).value or "?"
        print(f"  Row {r} ({name[:20]}): {', '.join(row_data)}")
