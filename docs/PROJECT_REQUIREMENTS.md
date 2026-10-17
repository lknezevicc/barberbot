# BarberBot - Functional Requirements

Status note: iCal export (F17), reminders (F18), multi-tenancy (F34) and the admin API and dashboard (bookings, employees, services, schedule) have been implemented since this list was written. The remaining items under "Planned features" are the roadmap.

## Implemented features

### F1. User registration through Telegram
The user starts the bot with `/start`. A session is created in Redis and the bot is ready to talk. The user does not have to configure anything.

### F2. Natural conversation with the bot (LLM)
The user writes free text in Croatian. The LLM recognises the intent and keeps the conversation natural, as if the user were talking to the reception desk of a barbershop.

### F3. Smart command recognition from free text
The user can write "What are my appointments?" instead of `/my_bookings`. The LLM recognises that the user wants to see their appointments and routes the message to the matching command. The other commands (`/cancel`, `/help`, `/start`) are covered as well.

### F4. Creating a booking through conversation
The bot collects the booking details (service, barber, date, time) step by step in a natural conversation. The user can give everything at once or one piece at a time. The LLM extracts the relevant data from every message and builds a draft.

### F5. Slot availability check
Before confirming, the bot checks that the requested slot is free for the chosen barber. It takes into account working hours (Mon-Fri 09-17, Sat 09-13, Sun closed), the duration of the service and existing bookings, with overlap detection on a 5-minute grid.

### F6. Suggesting alternative slots
If the requested slot is taken, the system automatically finds and offers up to 3 of the nearest free slots as inline buttons. It scans up to 14 days ahead and skips non-working days.

### F7. Temporary slot lock (hold)
When the bot offers a slot to the user for confirmation, that slot is held in the database for 2 minutes with status `HELD`. Other users cannot book the same slot while the hold lasts. If the user does not confirm in time, the hold expires automatically and the slot is released.

### F8. Booking confirmation with inline buttons
The user confirms or cancels the offered booking with one tap on the Telegram inline keyboard (Confirm / Cancel). No typing is needed.

### F9. User view of upcoming appointments
The user sends `/my_bookings` or asks something like "What are my appointments?" and gets a formatted list of all future confirmed bookings with date, time, service and barber.

### F10. Booking cancellation by the user
The user sends `/cancel` or says "I want to cancel my appointment". The bot shows a list of active appointments with inline buttons and the user cancels the chosen one with one tap. Both HELD and CONFIRMED bookings can be cancelled.

### F11. Input validation during booking
The bot rejects dates in the past and dates more than 30 days ahead, rounds the time to the nearest 5 minutes and normalises service names. Validation happens in the bot before the request is sent to the booking service.

### F12. Protection against prompt injection
Layered defence: input sanitisation (injection pattern detection, a length limit of 500 characters), safety boundaries in the system prompt, a canary token to detect a breach, and validation of the LLM response.

### F13. Per-user rate limiting
A Redis-based sliding-window rate limiter limits a user to 10 messages per minute. Excess messages get a warning.

### F14. Telegram update deduplication
An in-memory filter at the gateway prevents the same Telegram update from being processed twice. It uses `update_id` and cleans up old entries automatically.

### F15. Per-user message serialisation
A per-user Redis lock makes sure only one message per user is processed at a time. A stash-and-merge strategy joins quick consecutive text messages into a single LLM call.

### F16. Booking persistence in the database
All bookings are stored permanently in PostgreSQL with Flyway migrations. The data includes user, barber, service, date, time, status and duration.

---

## Planned features

### F17. Add the booking to a calendar (.ics)
When the user confirms a booking, the bot sends an `.ics` file (iCalendar format) as a Telegram document along with the confirmation message. The user adds the appointment to Google Calendar, Apple Calendar or Outlook with one tap. Covers generating the .ics file and sending the document through the Telegram API.

### F18. Notification before the appointment
The system automatically sends the user a Telegram reminder the day before and/or one hour before the appointment. A scheduled job in the booking service checks upcoming appointments and publishes a reply event.

### F19. Admin overview of all bookings
The admin panel shows a list of all bookings with filters by date, barber, status and user. It is a table with pagination and search. The admin sees who booked, when, with whom and for which service.

### F20. Admin cancels a booking
The admin can cancel any booking from the admin panel. The user automatically receives a Telegram message that the salon cancelled the appointment.

### F21. Admin reschedules a booking
The admin can change the date and/or time of an existing booking. The system checks the availability of the new slot. The user automatically receives a Telegram message with the new time.

### F22. Admin daily schedule (calendar view)
The admin panel shows the daily/weekly schedule per barber in a visual grid. Each slot is colour-coded (free, booked, held). The admin sees at a glance who has which appointment.

### F23. Barber catalogue management
The admin can add, edit and deactivate barbers in the admin panel. The bot uses the catalogue to validate barber names and to show available barbers to the user. CRUD operations take effect in the bot immediately.

### F24. Service catalogue management
The admin can define services (name, duration in minutes, price). The bot uses the catalogue to validate services and calculate the appointment duration. It replaces the hard-coded `ServiceDuration`.

### F25. Working hours management
The admin can set working hours per weekday, including special days (holidays, days off). It replaces the hard-coded `HardcodedWorkingHoursProvider` with a database-backed implementation.

### F26. User reschedules their own booking
The user says "I want to move my appointment" or uses a button on the inline keyboard. The bot shows the existing appointments, the user picks the one to move, and the bot then offers free alternative slots. The old appointment is cancelled automatically and a new one is created.

### F27. Admin dashboard with statistics
The admin panel shows key metrics: total number of bookings (daily/weekly/monthly), cancellation rate, most popular barber, most popular service, peak hours. Simple charts.

### F28. User profile with contact details
On the first booking the bot asks for the user's name and phone number. The data is stored in the database and linked to the Telegram user ID. The salon can contact the user outside Telegram if needed.

### F29. Service rating after the appointment
One hour after the appointment ends, the bot sends the user a message asking "How satisfied are you with the service?" with inline buttons for a rating (1-5 stars). Ratings are stored in the database and visible in the admin panel.

### F30. Admin notification about new bookings
When a user confirms a new booking, the admin automatically receives a notification (through Telegram or in the admin panel) with the booking details. Real-time notification without refreshing.

### F31. Blocked slots (breaks, lunch)
The admin can block specific time slots for a barber (lunch break, personal reasons). Blocked slots behave like booked ones, so the bot will not offer them to users.

### F32. Unit and integration tests
Coverage of the critical paths: slot availability engine (overlap detection, edge cases), booking flow (hold/confirm/cancel), handler pipeline (callback routing, validation), input sanitiser. Integration tests with Testcontainers for PostgreSQL, Redis and RabbitMQ.

### F33. Automatic conversation deletion (GDPR)
Conversation history in Redis expires automatically (TTL), while bookings in PostgreSQL stay. The admin can delete all data related to a user on request. Covers the right to be forgotten.

### F34. Multi-tenant support
Every barbershop gets its own `tenantId`. All entities, events and queries are filtered by tenant. Each shop can have its own Telegram bot, service catalogue, barbers and schedule. This is the foundation for a SaaS model.

### F35. Waitlist for full days
When all slots with the requested barber on the requested date are taken, the user can join a waitlist. If a slot is freed (a cancellation), the first user on the list automatically gets a notification with an offer.

### F36. Support for more communication channels
The architecture already allows this because Telegram is decoupled through the gateway. Adding WhatsApp or Viber only requires a new gateway adapter that translates incoming messages into the same `InboundMessage` format.

---

## Note on the admin panel

**Backend**: Spring Boot 3 + Spring Security + JWT (the same stack as the rest of the system, sharing models and the database).

**Frontend**: React + TypeScript + HeroUI v3 + Tailwind CSS v4. Vite for the build. TanStack Query for server state management.

The admin panel is a separate module (`admin-service` for the backend, `admin-dashboard` for the frontend) that talks to the same PostgreSQL database and to RabbitMQ for real-time notifications.
