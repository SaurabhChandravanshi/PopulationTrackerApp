# Country Population: Play Console drafts

Package: `com.i3developer.covid19cg` (unchanged). Category: Tools (keep, or Education / Reference if you prefer).
No health, outbreak or WhatsApp/Meta wording anywhere. Review every line before pasting.

## Store listing

**App name (30 max):** Country Population

**Short description (80 max):**
Official annual population figures for countries, with rankings and comparison.

**Full description:**
Country Population is a simple reference tool for looking up, ranking and comparing the total population of countries and territories.

• Search and sort 200+ countries by name, population, land area or density
• Country pages with a population chart from 1960 to the latest year available
• Rankings: largest, smallest, densest and fastest growing
• Compare up to four countries on one chart
• Save favourites on your device
• Works offline with built-in data; optionally refresh the latest figures

Data and dates: figures are annual estimates from World Bank Open Data (Population, total, SP.POP.TOTL; Land area, AG.LND.TOTL.K2). The app states the latest year covered on every screen. These are not live counts. Data is licensed CC BY 4.0. This app is not affiliated with or endorsed by the World Bank.

No account, no ads, no tracking.

**Release notes (first release):** First release of Country Population.

**Screenshots (take on a phone):** Countries list, a country page with chart, Rankings, Compare, About and sources.
**Icon (512x512):** store-assets/play-store-icon-512.png (see notes below).

## Declarations (answer truthfully; these match the app as built)

| Form | Answer |
|---|---|
| Health apps declaration | App has no health features |
| App access | All functionality available without login or special access |
| Ads | No ads |
| Target audience | 18+ or general audience; not designed for children |
| Content rating | Reference/utility; no violence, no user-generated content, no sharing of location |
| Data safety | Collects no user data; no data shared. Optional Refresh makes a request to api.worldbank.org (IP address is visible to that server). Confirm in Google's current Data safety guidance whether to declare this; the app sends no identifiers. Favourites stay on the device. |
| Government apps / Financial / News | No |
| Privacy policy URL | Required. Host the text below on a public non-PDF page, then set `PRIVACY_URL` in `AppViewModel.kt` |

## Privacy policy (draft; replace the contact placeholder)

Country Population ("the app") is published by <DEVELOPER NAME>. Contact: <EMAIL>.

The app does not require an account and does not collect, store or share personal data. It contains no advertising and no analytics. Your list of favourite countries is stored only on your device.

The app includes population data from World Bank Open Data. If you tap Refresh, the app requests the latest figures from api.worldbank.org over HTTPS. As with any internet request, the World Bank's server can see your IP address; the app sends no other information about you. The World Bank's own terms and privacy notice apply to that service.

The app does not knowingly collect data from children. Uninstalling the app removes all data it stored on your device. We may update this policy; the date of the latest update will be shown here. Last updated: <DATE>.

## Release checklist

1. Play Console: App integrity, App signing, **Request upload key reset**; wait for approval.
2. Generate the new upload keystore, create `keystore.properties` (gitignored) with `storeFile`, `storePassword`, `keyAlias`, `keyPassword`.
3. Check the highest version code ever uploaded for this package; `versionCode` is 1000, raise it if needed.
4. `./gradlew :app:bundleRelease` -> `app/build/outputs/bundle/release/app-release.aab` (targets API 36).
5. Update the listing, icon, screenshots and all declarations above before submitting for review.
6. Do not file another appeal or ticket for this app.

## Notes

- Icon: `store-assets/play-store-icon-512.png` is the full-bleed square 512x512 PNG from the AppIcons (5) set, which suits Play Console (Google applies the rounding).
- Refresh the bundled data: `python3 -I tools/build_data.py app/src/main/assets/population.json`.
- Region names and country names follow World Bank naming (a few shortened for display).
