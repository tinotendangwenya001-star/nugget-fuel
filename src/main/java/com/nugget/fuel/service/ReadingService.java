package com.nugget.fuel.service;

import com.nugget.fuel.dto.*;
import com.nugget.fuel.dto.Dtos.*;
import com.nugget.fuel.dto.ReadingResponse.FillDto;
import com.nugget.fuel.dto.ReadingResponse.Values;
import com.nugget.fuel.model.*;
import com.nugget.fuel.repo.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ReadingService {
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final Comparator<String> USD_FIRST =
            Comparator.comparing((String c) -> !c.equals("USD")).thenComparing(Comparator.naturalOrder());

    private final UserRepository users;
    private final PumpRepository pumps;
    private final ReadingRepository readings;
    private final StationRepository stations;
    private final CreditFillRepository fills;
    private final FuelPriceRepository priceRepo;
    private final CurrencyRepository currencies;
    private final CashDeclarationRepository cash;
    private final StockRepository stockRepo;

    @Value("${nugget.timezone:Africa/Harare}")
    private String timezone;

    public ReadingService(UserRepository users, PumpRepository pumps, ReadingRepository readings,
                          StationRepository stations, CreditFillRepository fills, FuelPriceRepository priceRepo,
                          CurrencyRepository currencies, CashDeclarationRepository cash,
                          StockRepository stockRepo) {
        this.users = users; this.pumps = pumps; this.readings = readings; this.stations = stations;
        this.fills = fills; this.priceRepo = priceRepo; this.currencies = currencies; this.cash = cash; this.stockRepo = stockRepo;
    }

    // ---------- helpers ----------
    public LocalDate today() { return LocalDate.now(ZoneId.of(timezone)); }
    private boolean isMonday() { return today().getDayOfWeek() == DayOfWeek.MONDAY; }
    private static BigDecimal nz(BigDecimal v) { return v == null ? ZERO : v; }
    private AppUser user(String username) { return users.findByUsername(username).orElseThrow(); }
    private ResponseStatusException bad(String msg) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg); }

    private Pump ownPump(AppUser a, Long pumpId) {
        Pump pump = pumps.findById(pumpId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pump not found"));
        if (!pump.getStation().getId().equals(a.getStation().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Pump belongs to another station");
        }
        return pump;
    }

    @Transactional(readOnly = true)
    public Map<String, String> me(String username) {
        AppUser u = user(username);
        return Map.of("name", u.getFullName(), "role", u.getRole().name(),
                "station", u.getStation() == null ? "All stations" : u.getStation().getName());
    }

    @Transactional(readOnly = true)
    public List<PumpDto> pumpsFor(String username) {
        AppUser u = user(username);
        List<Pump> list = u.getRole() == Role.MANAGER ? pumps.findAll() : pumps.findByStationId(u.getStation().getId());
        return list.stream().map(p -> new PumpDto(p.getId(), p.getName(), p.getFuelType(),
                p.getStation().getName(), priceOn(p.getFuelType(), today()))).toList();
    }

    // ---------- readings ----------
    @Transactional(readOnly = true)
    public PumpContext context(String username, Long pumpId) {
        Pump pump = ownPump(user(username), pumpId);
        LocalDate yesterday = today().minusDays(1);
        BigDecimal price = priceOn(pump.getFuelType(), today());
        boolean lunch = pump.isLunchPump(), transport = lunch && isMonday();
        var prev = readings.findFirstByPumpAndReadingDateLessThanOrderByReadingDateDesc(pump, today());
        if (!readings.existsByPump(pump) || prev.isEmpty()) {
            return new PumpContext(true, yesterday, null, null, pump.getFuelType(), price, lunch, transport);
        }
        FuelReading q = prev.get();
        return new PumpContext(false, yesterday, q.getReadingDate(),
                new Values(q.getClosingManual(), q.getClosingVolume(), q.getClosingSales()),
                pump.getFuelType(), price, lunch, transport);
    }

    public ReadingResponse submit(String username, ReadingRequest req) {
        AppUser attendant = user(username);
        Pump pump = ownPump(attendant, req.pumpId());
        LocalDate today = today();

        if (!readings.existsByPump(pump)) {   // first-time setup: previous day's opening + closing
            if (req.date() == null || !req.date().isBefore(today)) {
                throw bad("Choose the previous day's date for the setup readings.");
            }
            if (req.openingManual() == null || req.openingVolume() == null || req.openingSales() == null) {
                throw bad("Opening readings are required for the first entry of this pump.");
            }
            FuelReading r = new FuelReading(pump, attendant, req.date());
            r.set(attendant, req.date(),
                    req.openingManual(), req.openingVolume(), req.openingSales(),
                    req.closingManual(), req.closingVolume(), req.closingSales());
            return toResponse(readings.save(r));
        }

        FuelReading prev = readings.findFirstByPumpAndReadingDateLessThanOrderByReadingDateDesc(pump, today)
                .orElseThrow(() -> bad("No earlier closing reading found for this pump."));
        FuelReading r = readings.findByPumpAndReadingDate(pump, today)
                .orElseGet(() -> new FuelReading(pump, attendant, today));
        r.set(attendant, prev.getReadingDate(),
                prev.getClosingManual(), prev.getClosingVolume(), prev.getClosingSales(),
                req.closingManual(), req.closingVolume(), req.closingSales());
        boolean lunch = pump.isLunchPump();
        r.setExpenses(lunch ? nz(req.lunchAmount()) : ZERO, lunch && isMonday() ? nz(req.transportAmount()) : ZERO);
        return toResponse(readings.save(r));
    }

    public ReadingResponse addFill(String username, FillRequest req) {
        Pump pump = ownPump(user(username), req.pumpId());
        FuelReading r = readings.findByPumpAndReadingDate(pump, today())
                .orElseThrow(() -> bad("Save today's readings for this pump first."));
        String company = req.companyName() == null ? "" : req.companyName().trim();
        if (req.type() == CreditFill.Type.CREDITOR && company.isEmpty()) {
            throw bad("Enter the creditor's company name.");
        }
        fills.save(new CreditFill(r, req.type(), company, req.driverName().trim(),
                req.vehicleReg().trim().toUpperCase(), req.litres()));
        return toResponse(r);
    }

    public ReadingResponse deleteFill(String username, Long fillId) {
        CreditFill f = fills.findById(fillId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entry not found"));
        FuelReading r = f.getReading();
        ownPump(user(username), r.getPump().getId());
        if (!r.getReadingDate().equals(today())) throw bad("Only today's entries can be removed.");
        fills.delete(f);
        return toResponse(r);
    }

    @Transactional(readOnly = true)
    public List<ReadingResponse> mine(String username, LocalDate date) {
        AppUser u = user(username);
        return readings.findByAttendantIdAndReadingDate(u.getId(), date == null ? today() : date)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ReadingResponse> forManager(LocalDate date, Long stationId) {
        return readings.findForManager(date == null ? today() : date, stationId)
                .stream().map(this::toResponse).toList();
    }

    // ---------- fuel prices (set by the manager; stay in force until changed) ----------
    private Optional<FuelPrice> priceRow(String fuelType, LocalDate date) {
        return priceRepo.findFirstByFuelTypeAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(fuelType, date)
                .or(() -> priceRepo.findFirstByFuelTypeOrderByEffectiveDateAsc(fuelType));
    }

    private BigDecimal priceOn(String fuelType, LocalDate date) {
        return priceRow(fuelType, date).map(FuelPrice::getPricePerLitre).orElse(ZERO);
    }

    @Transactional(readOnly = true)
    public List<PriceInfo> prices() {
        LocalDate t = today();
        return pumps.findAll().stream().map(Pump::getFuelType).distinct().sorted().map(type -> {
            var cur = priceRow(type, t);
            var next = priceRepo.findFirstByFuelTypeAndEffectiveDateGreaterThanOrderByEffectiveDateAsc(type, t);
            return new PriceInfo(type, cur.map(FuelPrice::getPricePerLitre).orElse(ZERO),
                    cur.map(FuelPrice::getEffectiveDate).orElse(null),
                    next.map(FuelPrice::getPricePerLitre).orElse(null),
                    next.map(FuelPrice::getEffectiveDate).orElse(null));
        }).toList();
    }

    public List<PriceInfo> setPrice(String username, PriceRequest req) {
        String type = req.fuelType().trim().toUpperCase();
        if (pumps.findAll().stream().noneMatch(p -> p.getFuelType().equals(type))) throw bad("Unknown fuel type.");
        LocalDate date = req.effectiveDate() == null ? today() : req.effectiveDate();
        if (date.isBefore(today())) throw bad("A new price cannot start in the past.");
        FuelPrice fp = priceRepo.findByFuelTypeAndEffectiveDate(type, date).orElseGet(() -> new FuelPrice(type, date));
        fp.set(req.pricePerLitre(), username);
        priceRepo.save(fp);
        return prices();
    }

    // ---------- currencies and branches (manager) ----------
    @Transactional(readOnly = true)
    public List<StationInfo> stationInfos() {
        return stations.findAll().stream().map(s -> new StationInfo(s.getId(), s.getName(),
                s.getCurrencies().stream().sorted(USD_FIRST).toList())).toList();
    }

    private CurrencyInfo cinfo(Currency c) { return new CurrencyInfo(c.getCode(), c.getName(), c.getUnitsPerUsd()); }

    @Transactional(readOnly = true)
    public List<CurrencyInfo> currencyList() {
        return currencies.findAll().stream().map(this::cinfo)
                .sorted(Comparator.comparing(CurrencyInfo::code, USD_FIRST)).toList();
    }

    /** Add a currency or update its exchange rate (units per US$1). */
    public List<CurrencyInfo> saveCurrency(String username, CurrencyRequest req) {
        String code = req.code().trim().toUpperCase();
        BigDecimal rate = code.equals("USD") ? BigDecimal.ONE : req.unitsPerUsd();
        Currency c = currencies.findById(code).orElseGet(() -> new Currency(code, req.name().trim(), rate));
        c.set(req.name().trim(), rate, username);
        currencies.save(c);
        return currencyList();
    }

    public List<StationInfo> setStationCurrencies(Long stationId, StationCurrenciesRequest req) {
        Station st = stations.findById(stationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
        Set<String> clean = new TreeSet<>();
        clean.add("USD");
        for (String c : req.currencies()) {
            String code = c.trim().toUpperCase();
            if (!currencies.existsById(code)) throw bad("Unknown currency " + code);
            clean.add(code);
        }
        st.setCurrencies(clean);
        stations.save(st);
        return stationInfos();
    }

    // ---------- attendant: cash handed in, by currency ----------
    private BigDecimal toUsd(BigDecimal amount, BigDecimal rate) {
        return amount.divide(rate, 2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public CashSheet cashSheet(String username, LocalDate date) {
        AppUser u = user(username);
        LocalDate d = date == null ? today() : date;
        List<CurrencyInfo> accepted = u.getStation().getCurrencies().stream().sorted(USD_FIRST)
                .map(code -> currencies.findById(code)).flatMap(Optional::stream).map(this::cinfo).toList();
        List<CashLine> lines = cash.findByAttendantIdAndDeclDate(u.getId(), d).stream()
                .map(x -> new CashLine(x.getCurrencyCode(), x.getAmount(), x.getRate(), toUsd(x.getAmount(), x.getRate())))
                .sorted(Comparator.comparing(CashLine::currency, USD_FIRST)).toList();
        BigDecimal declared = lines.stream().map(CashLine::usdValue).reduce(ZERO, BigDecimal::add);
        BigDecimal expected = readings.findByAttendantIdAndReadingDate(u.getId(), d).stream()
                .map(r -> toResponse(r).cashTotal()).reduce(ZERO, BigDecimal::add);
        return new CashSheet(lines, accepted, declared, expected);
    }

    public CashSheet saveCash(String username, CashRequest req) {
        AppUser u = user(username);
        LocalDate d = today();
        Map<String, BigDecimal> merged = new LinkedHashMap<>();
        for (CashLineRequest l : req.lines()) {
            String code = l.currency().trim().toUpperCase();
            if (!u.getStation().getCurrencies().contains(code)) throw bad("Your branch does not accept " + code + ".");
            if (l.amount().signum() > 0) merged.merge(code, l.amount(), BigDecimal::add);
        }
        cash.deleteByAttendantIdAndDeclDate(u.getId(), d);
        cash.flush();
        for (var e : merged.entrySet()) {
            Currency c = currencies.findById(e.getKey()).orElseThrow(() -> bad("Unknown currency " + e.getKey()));
            cash.save(new CashDeclaration(u, d, e.getKey(), e.getValue(), c.getUnitsPerUsd()));
        }
        return cashSheet(username, d);
    }

    // ---------- manager: the daily branch summary ----------
    @Transactional(readOnly = true)
    public StationSummary stationSummary(Long stationId, LocalDate date) {
        Station st = stations.findById(stationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
        LocalDate d = date == null ? today() : date;
        List<FuelReading> rows = readings.findForManager(d, stationId);
        List<CashDeclaration> decl = cash.findByDeclDateAndAttendantStationId(d, stationId);

        BigDecimal sales = ZERO, expected = ZERO, companyFuel = ZERO, lunch = ZERO, transport = ZERO, credit = ZERO;
        Map<String, BigDecimal> creditors = new TreeMap<>(), litres = new TreeMap<>();
        Map<String, BigDecimal> expByPerson = new TreeMap<>(), decByPerson = new TreeMap<>();
        for (FuelReading r : rows) {
            ReadingResponse x = toResponse(r);
            sales = sales.add(x.difference().sales());
            expected = expected.add(x.cashTotal());
            lunch = lunch.add(x.lunchAmount());
            transport = transport.add(x.transportAmount());
            litres.merge(r.getPump().getFuelType(), x.difference().volume(), BigDecimal::add);
            expByPerson.merge(x.attendant(), x.cashTotal(), BigDecimal::add);
            for (FillDto f : x.fills()) {
                if (f.type().equals("COMPANY")) {
                    companyFuel = companyFuel.add(f.amount());
                } else {
                    credit = credit.add(f.amount());
                    creditors.merge(f.companyName().isBlank() ? f.driverName() : f.companyName(), f.amount(), BigDecimal::add);
                }
            }
        }

        Map<String, BigDecimal> amt = new TreeMap<>(), usd = new TreeMap<>();
        BigDecimal declared = ZERO;
        for (CashDeclaration c : decl) {
            BigDecimal u = toUsd(c.getAmount(), c.getRate());
            amt.merge(c.getCurrencyCode(), c.getAmount(), BigDecimal::add);
            usd.merge(c.getCurrencyCode(), u, BigDecimal::add);
            decByPerson.merge(c.getAttendant().getFullName(), u, BigDecimal::add);
            declared = declared.add(u);
        }
        Set<String> codes = new TreeSet<>(st.getCurrencies());
        codes.addAll(amt.keySet());
        List<CurrencyTotal> cashRows = codes.stream().sorted(USD_FIRST).map(code -> {
            Currency c = currencies.findById(code).orElse(null);
            return new CurrencyTotal(code, c == null ? code : c.getName(), amt.getOrDefault(code, ZERO),
                    c == null ? BigDecimal.ONE : c.getUnitsPerUsd(), usd.getOrDefault(code, ZERO));
        }).toList();

        Set<String> people = new TreeSet<>(expByPerson.keySet());
        people.addAll(decByPerson.keySet());
        List<AttendantCash> attendants = people.stream()
                .map(n -> new AttendantCash(n, expByPerson.getOrDefault(n, ZERO), decByPerson.getOrDefault(n, ZERO))).toList();

        return new StationSummary(st.getId(), st.getName(), d, rows.size(), sales, expected, cashRows, declared,
                companyFuel, lunch, transport, companyFuel.add(lunch).add(transport),
                credit, creditors.entrySet().stream().map(e -> new CreditLine(e.getKey(), e.getValue())).toList(),
                litres.entrySet().stream().map(e -> new FuelLitres(e.getKey(), e.getValue())).toList(), attendants,
                stockSheet(stationId, d));
    }

    // ---------- stock (supervisor) ----------
    // First day: the supervisor types in the opening and closing stock. After that:
    //   opening = previous day's closing
    //   closing = opening + deliveries + returns - litres sold by ALL pumps of that fuel
    private Map<LocalDate, BigDecimal> soldByDay(Long sid, String type, LocalDate from, LocalDate to) {
        Map<LocalDate, BigDecimal> m = new HashMap<>();
        for (Object[] row : readings.soldByDay(sid, type, from, to)) {
            m.put((LocalDate) row[0], new BigDecimal(row[1].toString()));
        }
        return m;
    }

    @Transactional(readOnly = true)
    public List<StockLine> stockSheet(Long stationId, LocalDate date) {
        LocalDate d = date == null ? today() : date;
        return pumps.findByStationId(stationId).stream().map(Pump::getFuelType).distinct().sorted()
                .map(t -> stockLine(stationId, t, d)).toList();
    }

    private StockLine stockLine(Long sid, String t, LocalDate d) {
        BigDecimal soldToday = soldByDay(sid, t, d, d).getOrDefault(d, ZERO);
        var base = stockRepo.findFirstByStationIdAndFuelTypeAndBaselineTrueOrderByStockDateAsc(sid, t);
        if (base.isEmpty()) return new StockLine(t, true, false, false, null, null, ZERO, ZERO, soldToday, null);
        StockEntry b = base.get();
        LocalDate bd = b.getStockDate();
        if (d.isBefore(bd)) return new StockLine(t, false, true, false, bd, null, ZERO, ZERO, soldToday, null);
        if (d.equals(bd)) {
            return new StockLine(t, false, false, true, bd, b.getOpeningStock(), ZERO, ZERO, soldToday, b.getClosingStock());
        }

        // after the first day: roll the stock forward day by day
        Map<LocalDate, StockEntry> moves = new HashMap<>();
        for (StockEntry e : stockRepo.findByStationIdAndFuelTypeAndBaselineFalseAndStockDateBetween(
                sid, t, bd.plusDays(1), d)) {
            moves.put(e.getStockDate(), e);
        }
        Map<LocalDate, BigDecimal> sold = soldByDay(sid, t, bd.plusDays(1), d);
        BigDecimal closing = b.getClosingStock(), opening = closing, del = ZERO, ret = ZERO, soldD = ZERO;
        for (LocalDate x = bd.plusDays(1); !x.isAfter(d); x = x.plusDays(1)) {
            opening = closing;
            StockEntry m = moves.get(x);
            del = m == null ? ZERO : m.getDeliveries();
            ret = m == null ? ZERO : m.getReturns();
            soldD = sold.getOrDefault(x, ZERO);
            closing = opening.add(del).add(ret).subtract(soldD);
        }
        return new StockLine(t, false, false, false, bd, opening, del, ret, soldD, closing);
    }

    private Long supervisorStation(String username) {
        AppUser u = user(username);
        if (u.getRole() != Role.SUPERVISOR || u.getStation() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Supervisors only");
        }
        return u.getStation().getId();
    }

    @Transactional(readOnly = true)
    public List<StockLine> supervisorStock(String username, LocalDate date) {
        return stockSheet(supervisorStation(username), date);
    }

    @Transactional(readOnly = true)
    public StationSummary supervisorSummary(String username, LocalDate date) {
        return stationSummary(supervisorStation(username), date);
    }

    public List<StockLine> saveStock(String username, StockRequest req) {
        Long sid = supervisorStation(username);
        Station station = stations.findById(sid).orElseThrow();
        String type = req.fuelType().trim().toUpperCase();
        if (pumps.findByStationId(sid).stream().noneMatch(p -> p.getFuelType().equals(type))) {
            throw bad("This branch has no " + type + " pumps.");
        }
        LocalDate d = req.date() == null ? today() : req.date();
        if (d.isAfter(today())) throw bad("The date cannot be in the future.");

        var base = stockRepo.findFirstByStationIdAndFuelTypeAndBaselineTrueOrderByStockDateAsc(sid, type);
        if (base.isEmpty() || d.equals(base.get().getStockDate())) {
            // first day (or correcting it): opening and closing are typed in
            if (req.opening() == null || req.closing() == null) {
                throw bad("Enter the opening and closing stock for the first day.");
            }
            StockEntry e = base.orElseGet(() -> new StockEntry(station, type, d, true));
            e.setBaseline(req.opening(), req.closing(), username);
            stockRepo.save(e);
        } else if (d.isAfter(base.get().getStockDate())) {
            // later days: only deliveries and returns are typed in
            StockEntry e = stockRepo.findByStationIdAndFuelTypeAndStockDate(sid, type, d)
                    .orElseGet(() -> new StockEntry(station, type, d, false));
            e.setMovements(nz(req.deliveries()), nz(req.returns()), username);
            stockRepo.save(e);
        } else {
            throw bad("That date is before the first stock day.");
        }
        return stockSheet(sid, d);
    }

    // ---------- supervisor: every attendant's page at the branch (read only) ----------
    @Transactional(readOnly = true)
    public List<AttendantPage> supervisorPages(String username, LocalDate date) {
        return branchPages(supervisorStation(username), date);
    }

    private List<AttendantPage> branchPages(Long stationId, LocalDate date) {
        LocalDate d = date == null ? today() : date;
        Map<String, List<ReadingResponse>> byPerson = new TreeMap<>();
        for (FuelReading r : readings.findForManager(d, stationId)) {
            ReadingResponse x = toResponse(r);
            byPerson.computeIfAbsent(x.attendant(), k -> new ArrayList<>()).add(x);
        }
        Map<String, List<CashLine>> cashBy = new TreeMap<>();
        for (CashDeclaration c : cash.findByDeclDateAndAttendantStationId(d, stationId)) {
            cashBy.computeIfAbsent(c.getAttendant().getFullName(), k -> new ArrayList<>())
                    .add(new CashLine(c.getCurrencyCode(), c.getAmount(), c.getRate(), toUsd(c.getAmount(), c.getRate())));
        }
        Set<String> names = new TreeSet<>(byPerson.keySet());
        names.addAll(cashBy.keySet());
        return names.stream().map(n -> {
            List<ReadingResponse> rs = byPerson.getOrDefault(n, List.of());
            List<CashLine> cl = cashBy.getOrDefault(n, List.of());
            return new AttendantPage(n, rs, cl,
                    rs.stream().map(ReadingResponse::cashTotal).reduce(ZERO, BigDecimal::add),
                    cl.stream().map(CashLine::usdValue).reduce(ZERO, BigDecimal::add));
        }).toList();
    }

    // ---------- mapping ----------
    private ReadingResponse toResponse(FuelReading r) {
        BigDecimal price = priceOn(r.getPump().getFuelType(), r.getReadingDate());
        Values o = new Values(r.getOpeningManual(), r.getOpeningVolume(), r.getOpeningSales());
        Values c = new Values(r.getClosingManual(), r.getClosingVolume(), r.getClosingSales());
        Values d = new Values(c.manual().subtract(o.manual()), c.volume().subtract(o.volume()),
                c.sales().subtract(o.sales()));
        List<FillDto> list = fills.findByReadingIdOrderById(r.getId()).stream()
                .map(f -> new FillDto(f.getId(), f.getType().name(),
                        f.getCompanyName() == null ? "" : f.getCompanyName(), f.getDriverName(), f.getVehicleReg(),
                        f.getLitres(), f.getLitres().multiply(price).setScale(2, RoundingMode.HALF_UP)))
                .toList();
        BigDecimal credit = list.stream().map(FillDto::amount).reduce(ZERO, BigDecimal::add);
        return new ReadingResponse(r.getId(), r.getPump().getStation().getName(), r.getPump().getName(),
                r.getAttendant().getFullName(), r.getReadingDate(),
                r.getOpeningDate().equals(r.getReadingDate()), r.getOpeningDate(), price,
                o, c, d, list, credit, d.sales().subtract(credit),
                nz(r.getLunchAmount()), nz(r.getTransportAmount()));
    }
}
