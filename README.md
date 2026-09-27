# CirrusNav
Safely travel and account for the unpredictability of weather

## Tasks (Choose whatever to work on)
### MVP
- [x] Map
      - https://github.com/afarber/OpenMapView
- [x] Path to destination
      - (Google Maps or alt?)
- [X] Time estimate along path (allow dynamic updating)
      - Weather at location X time away
- [X] Weather at each position on the route, accounting for time
      - https://www.weatherapi.com/pricing.aspx
      - Only for certain points. 33, 66, 100%

### Extending
- [ ] Overlay weather map over time
      - https://weathermaps.weatherapi.com/precip/tiles/map.html
- [ ] GPS Position
      - ???


## Pitch
Stop letting unpredictable weather dictate your commute. Do you really need to pack that rain jacket?  Should you take that scenic route? <!--- and sweater ---> Or, can you travel light and navigate your day with peace of mind? We built CirrusNav to answer these questions!

## Inspiration
Difficult weather can be very difficult to plan for, especially on long commutes. Our apps serves as a solution to this overlooked but crucial issue.

For longer road trips, our dynamic weather report tells you weather to speed up or slow down to dodge incoming storms entirely, letting you travel with complete serenity.

## What it does
CirrusNav is a smart navigation app. Instead of just showing you the current weather across a map, CirrusNav calculates your exact driving route and estimates the weather at various checkpoints along the way.

It queries hourly forecast data to show you exactly what the weather will be at those coordinates at your exact time of arrival. With the intuitive map, you can instantly see if you'll be driving into a thunderstorm 3 hours from now.

## How we built it
We built the Android app entirely in Kotlin.
- Mapping Engine: We used OpenMapView to render lightweight OpenStreetMap tiles.
- Routing & Geocoding: We chained together Nominatim (to convert city names to coordinates) and the OpenRouteService (ORS) API to fetch the driving path, total distance, and duration.
- Weather: We used WeatherAPI.com to pull deep, 48-hour forecasting data.

## Challenges we ran into
Ran into a critical bug inside the third-party mapping library's GeoJSON parser that crashed the app when reading route data. To fix this a bypass was used to bypass the library and extract the coordinate arrays safely.

## Accomplishments that we're proud of
- Making it an open source solution for users that don't like big tech.
- Prioritizing our user's experience by making the map intuitive and easy to understand.


## What we learned
- Kotlin
- Android Development
- collaborating using Git and Github.
- Proper Documentation
- Working with a team under time constraints

## What's next for CirrusNav
- Dynamic Rerouting: Calculating and suggesting an alternate route if it means avoiding a severe storm, even if the drive is slightly longer.
- Push Notifications: Alerting the user to "leave 15 minutes earlier to avoid the rain."
- Different Transport Options: Adding weather routing for cycling and walking

# NAVIGATE THE CLOUDS WITH CirrusNAV!