#!/usr/bin/env python3
"""Builds app/src/main/assets/population.json from the World Bank Indicators API.

Data: World Bank Open Data, CC BY 4.0.
  SP.POP.TOTL     total population (annual)
  AG.LND.TOTL.K2  land area, sq. km (latest available year)
"""
import json
import sys
import time
import urllib.request

API = "https://api.worldbank.org/v2"
FIRST_YEAR = 1960
LAST_YEAR = time.gmtime().tm_year

# Short display names for World Bank style names.
RENAME = {
    "Korea, Rep.": "South Korea",
    "Korea, Dem. People's Rep.": "North Korea",
    "Russian Federation": "Russia",
    "Egypt, Arab Rep.": "Egypt",
    "Iran, Islamic Rep.": "Iran",
    "Venezuela, RB": "Venezuela",
    "Yemen, Rep.": "Yemen",
    "Syrian Arab Republic": "Syria",
    "Lao PDR": "Laos",
    "Kyrgyz Republic": "Kyrgyzstan",
    "Slovak Republic": "Slovakia",
    "Gambia, The": "Gambia",
    "Bahamas, The": "Bahamas",
    "Congo, Dem. Rep.": "DR Congo",
    "Congo, Rep.": "Congo",
    "Hong Kong SAR, China": "Hong Kong",
    "Macao SAR, China": "Macao",
    "Micronesia, Fed. Sts.": "Micronesia",
    "St. Kitts and Nevis": "Saint Kitts and Nevis",
    "St. Lucia": "Saint Lucia",
    "St. Vincent and the Grenadines": "Saint Vincent and the Grenadines",
    "St. Martin (French part)": "Saint Martin (French part)",
    "Viet Nam": "Vietnam",
    "Turkiye": "Türkiye",
}


def get(path):
    with urllib.request.urlopen(f"{API}/{path}", timeout=60) as r:
        return json.load(r)


def main(out):
    meta = get("country?format=json&per_page=400")[1]
    countries = {
        c["id"]: c for c in meta if c["region"]["value"].strip() != "Aggregates"
    }

    pop_page = get(
        f"country/all/indicator/SP.POP.TOTL?format=json&per_page=20000&date={FIRST_YEAR}:{LAST_YEAR}"
    )
    updated = pop_page[0]["lastupdated"]
    series = {}
    for row in pop_page[1]:
        iso = row["countryiso3code"]
        if iso in countries and row["value"] is not None:
            series.setdefault(iso, {})[int(row["date"])] = int(row["value"])

    area_page = get(
        "country/all/indicator/AG.LND.TOTL.K2?format=json&per_page=20000&date=2000:%d" % LAST_YEAR
    )
    area = {}
    for row in area_page[1]:
        iso = row["countryiso3code"]
        if iso in countries and row["value"] is not None:
            year = int(row["date"])
            if iso not in area or year > area[iso][1]:
                area[iso] = (float(row["value"]), year)

    last_year = max(max(s) for s in series.values())
    out_countries = []
    for iso, c in sorted(countries.items(), key=lambda kv: kv[1]["name"]):
        if iso not in series:
            continue
        s = series[iso]
        values = [s.get(y) for y in range(FIRST_YEAR, last_year + 1)]
        name = RENAME.get(c["name"], c["name"])
        out_countries.append(
            {
                "i": iso,
                "c": c["iso2Code"],
                "n": name,
                "r": c["region"]["value"].strip(),
                "inc": c["incomeLevel"]["value"].strip(),
                "cap": c["capitalCity"].strip(),
                "a": area.get(iso, (None, None))[0],
                "ay": area.get(iso, (None, None))[1],
                "p": values,
            }
        )

    doc = {
        "firstYear": FIRST_YEAR,
        "lastYear": last_year,
        "sourceUpdated": updated,
        "countries": out_countries,
    }
    with open(out, "w", encoding="utf-8") as f:
        json.dump(doc, f, ensure_ascii=False, separators=(",", ":"))
    print(f"{len(out_countries)} countries, data to {last_year}, source updated {updated}")


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "app/src/main/assets/population.json")
