# BarberBot - Funkcionalni zahtjevi

## Implementirane funkcionalnosti

### F1. Registracija korisnika putem Telegrama
Korisnik pokreće bota s `/start`. Sesija se kreira u Redisu i bot je spreman za komunikaciju. Korisnik ne mora ništa dodatno konfigurirati.

### F2. Prirodna komunikacija s botom (LLM)
Korisnik piše slobodnim tekstom na hrvatskom jeziku. LLM prepoznaje namjeru i vodi razgovor prirodno, kao da korisnik razgovara s recepcijom frizerskog salona.

### F3. Pametno prepoznavanje naredbi iz teksta
Korisnik može napisati "Koji su moji termini?" umjesto `/my_bookings`. LLM prepoznaje da korisnik želi vidjeti termine i automatski prosljeđuje na odgovarajuću komandu. Pokriva i ostale komande (`/cancel`, `/help`, `/start`).

### F4. Kreiranje rezervacije kroz razgovor
Bot progresivno prikuplja podatke za rezervaciju (usluga, frizer, datum, vrijeme) kroz prirodan razgovor. Korisnik može dati sve podatke odjednom ili ih davati jedan po jedan. LLM ekstrahira relevantne podatke iz svake poruke i gradi draft.

### F5. Provjera dostupnosti termina
Prije potvrde bot provjerava je li traženi termin slobodan kod odabranog frizera. Uzima u obzir radno vrijeme (pon-pet 09-17, sub 09-13, ned zatvoreno), trajanje usluge i postojeće rezervacije s overlap detekcijom na 5-minutnoj mreži.

### F6. Predlaganje alternativnih termina
Ako je traženi termin zauzet, sustav automatski pronalazi i nudi do 3 najbliža slobodna termina kao inline gumbe. Skenira do 14 dana unaprijed, preskačući neradne dane.

### F7. Privremeno zaključavanje termina (hold)
Kada bot ponudi termin korisniku na potvrdu, taj slot se drži 2 minute u bazi sa statusom `HELD`. Drugi korisnici ne mogu rezervirati isti termin dok hold traje. Ako korisnik ne potvrdi u roku, hold automatski istječe i termin se oslobađa.

### F8. Potvrda rezervacije inline gumbima
Korisnik potvrđuje ili otkazuje ponuđenu rezervaciju jednim klikom na Telegram inline keyboard gumbe (Potvrdi / Odustani). Nema potrebe za pisanjem teksta.

### F9. Pregled nadolazećih termina od strane korisnika
Korisnik koristi `/my_bookings` ili postavlja pitanje poput "Koji su moji termini?" i dobije formatirani popis svih budućih potvrđenih rezervacija s datumom, vremenom, uslugom i frizerom.

### F10. Otkazivanje rezervacije od strane korisnika
Korisnik koristi `/cancel` ili kaže "Želim otkazati termin". Bot prikazuje listu aktivnih termina s inline gumbima i korisnik jednim klikom otkaže odabrani termin. Sustav podržava otkazivanje i HELD i CONFIRMED rezervacija.

### F11. Validacija unosa pri rezervaciji
Bot odbija datume u prošlosti, datume dalje od 30 dana, zaokružuje vrijeme na najbližih 5 minuta i normalizira nazive usluga. Validacija se vrši na bot razini prije slanja zahtjeva booking servisu.

### F12. Zaštita od prompt injection napada
Višeslojna obrana: sanitizacija ulaza (detekcija injection uzoraka, ograničenje duljine na 500 znakova), sigurnosne granice u system promptu, canary token za detekciju proboja, validacija LLM odgovora.

### F13. Rate limiting po korisniku
Redis-bazirani sliding window rate limiter ograničava korisnika na 10 poruka po minuti. Prekomjerne poruke dobivaju upozorenje.

### F14. Dedupliciranje Telegram ažuriranja
In-memory filter na gateway razini sprječava dvostruko procesiranje istog Telegram update-a. Koristi `update_id` s automatskim čišćenjem starih zapisa.

### F15. Serijalizacija poruka po korisniku
Per-user Redis lock osigurava da se samo jedna poruka po korisniku obrađuje istovremeno. Stash-and-merge strategija spaja brze uzastopne tekstualne poruke u jedan LLM poziv.

### F16. Perzistencija rezervacija u bazi podataka
Sve rezervacije se trajno pohranjuju u PostgreSQL bazi s Flyway migracijama. Podaci uključuju korisnika, frizer, uslugu, datum, vrijeme, status i trajanje.

---

## Funkcionalnosti za implementaciju

### F17. Dodavanje rezervacije u kalendar (.ics)
Kada korisnik potvrdi rezervaciju, bot uz poruku potvrde šalje i `.ics` datoteku (iCalendar format) kao Telegram dokument. Korisnik jednim klikom dodaje termin u Google Calendar, Apple Calendar ili Outlook. Pokriva: generiranje .ics datoteke, slanje dokumenta putem Telegram API-ja.

### F18. Notifikacija korisniku prije termina
Sustav šalje automatski podsjetnik korisniku putem Telegram poruke dan prije i/ili sat vremena prije zakazanog termina. Scheduled job u booking-service provjerava nadolazeće termine i publishira reply event.

### F19. Admin pregled svih rezervacija
Admin panel prikazuje listu svih rezervacija s filterima po datumu, frizeru, statusu i korisniku. Tablični prikaz s paginacijom i pretragom. Admin vidi tko je rezervirao, kada, kod koga i za koju uslugu.

### F20. Admin otkazivanje rezervacije
Admin može otkazati bilo koju rezervaciju iz admin panela. Korisnik automatski dobiva Telegram obavijest da je njegov termin otkazan od strane salona.

### F21. Admin premještanje rezervacije
Admin može promijeniti datum i/ili vrijeme postojeće rezervacije. Sustav provjerava dostupnost novog termina. Korisnik automatski dobiva Telegram obavijest s novim terminom.

### F22. Admin dnevni raspored (kalendarski prikaz)
Admin panel prikazuje dnevni/tjedni raspored po frizeru u vizualnom grid formatu. Svaki slot je bojom označen (slobodan, zauzet, held). Admin na prvi pogled vidi tko ima koji termin.

### F23. Upravljanje katalogom frizera
Admin može dodavati, uređivati i deaktivirati frizere kroz admin panel. Bot koristi katalog za validaciju imena frizera i prikaz dostupnih frizera korisniku. CRUD operacije s immediate efektom na bot.

### F24. Upravljanje katalogom usluga
Admin može definirati usluge (naziv, trajanje u minutama, cijena). Bot koristi katalog za validaciju usluga i izračun trajanja termina. Zamjenjuje hardkodirani `ServiceDuration`.

### F25. Upravljanje radnim vremenom
Admin može postaviti radno vrijeme po danu u tjednu, uključujući posebne dane (praznici, slobodni dani). Zamjenjuje hardkodirani `HardcodedWorkingHoursProvider` s DB-backed implementacijom.

### F26. Korisnik može premjestiti svoju rezervaciju
Korisnik kaže "Želim premjestiti termin" ili koristi gumb na inline keyboardu. Bot prikazuje postojeće termine, korisnik odabire koji želi premjestiti, a zatim nudi slobodne alternativne termine. Stari termin se automatski otkazuje, novi se kreira.

### F27. Admin dashboard sa statistikama
Admin panel prikazuje ključne metrike: ukupan broj rezervacija (dnevno/tjedno/mjesečno), stopa otkazivanja, najpopularniji frizer, najpopularnija usluga, vršni sati. Jednostavni grafovi.

### F28. Profil korisnika s kontakt podacima
Prilikom prve rezervacije bot traži ime i broj telefona korisnika. Podaci se spremaju u bazu i povezuju s Telegram userId-em. Salon može kontaktirati korisnika izvan Telegrama ako je potrebno.

### F29. Ocjenjivanje usluge nakon termina
Sat vremena nakon završetka termina, bot šalje korisniku poruku s pitanjem "Kako si zadovoljan uslugom?" s inline gumbima za ocjenu (1-5 zvjezdica). Ocjene se spremaju u bazu i vidljive su u admin panelu.

### F30. Admin obavijest o novim rezervacijama
Kada korisnik potvrdi novu rezervaciju, admin automatski dobiva obavijest (putem Telegrama ili u admin panelu) s detaljima rezervacije. Real-time notifikacija bez potrebe za osvježavanjem.

### F31. Blokirani termini (pauze, ručak)
Admin može blokirati određene vremenske slotove za pojedinog frizera (pauza za ručak, osobni razlozi). Blokirani slotovi se ponašaju kao zauzeti -- bot ih neće nuditi korisnicima.

### F32. Unit i integracijski testovi
Pokrivanje kritičnih putanja: slot availability engine (overlap detekcija, edge cases), booking flow (hold/confirm/cancel), handler pipeline (callback routing, validacija), input sanitizer. Integration testovi s Testcontainers za PostgreSQL, Redis i RabbitMQ.

### F33. Automatsko brisanje razgovora (GDPR)
Konverzacijska povijest u Redisu automatski istječe (TTL), ali rezervacije u PostgreSQL ostaju. Admin može obrisati sve podatke vezane za korisnika na zahtjev. Pokriva pravo na zaborav.

### F34. Multi-tenant podrška
Svaki frizerski salon dobiva vlastiti `tenantId`. Sve entitete, evente i upite filtriramo po tenant-u. Svaki salon može imati vlastiti Telegram bot, katalog usluga, frizere i raspored. Temelj za SaaS model.

### F35. Waitlist za pune dane
Kada su svi termini kod traženog frizera na traženi datum zauzeti, korisnik se može staviti na listu čekanja. Ako se termin oslobodi (otkazivanje), prvi korisnik na listi automatski dobiva obavijest s ponudom.

### F36. Podrška za više komunikacijskih kanala
Arhitektura već podržava proširenje jer je Telegram decoupliran kroz gateway. Dodavanje WhatsApp ili Viber kanala zahtijeva samo novi gateway adapter koji prevodi dolazne poruke u isti `InboundMessage` format.

---

## Napomena o admin panelu

**Backend**: Spring Boot 3 + Spring Security + JWT (isti stack kao ostatak sustava, dijeli modele i bazu).

**Frontend**: React + TypeScript + HeroUI v3 + Tailwind CSS v4. Vite za build. TanStack Query za server state management.

Admin panel je zasebni modul (`admin-service` za backend, `admin-frontend` za frontend) koji komunicira s istom PostgreSQL bazom i RabbitMQ-om za real-time obavijesti.
