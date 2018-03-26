package id.ac.tazkia.kinerja.aplikasikinerja.controller;

import id.ac.tazkia.kinerja.aplikasikinerja.constants.CategoryConstants;
import id.ac.tazkia.kinerja.aplikasikinerja.dao.*;
import id.ac.tazkia.kinerja.aplikasikinerja.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Controller
public class DaftarBawahanController {
    private static final Logger LOGGER = LoggerFactory.getLogger(DaftarBawahanController.class);

    @Autowired
    private UserDao userDao;

    @Autowired
    private StaffDao staffDao;

    @Autowired
    private StaffKpiDao staffKpiDao;

    @Autowired
    private IndicatorsDao indicatorsDao;

    @Autowired
    private ScoreDao scoreDao;


    @Value("${upload.folder}")
    private String uploadFolder;

    private Category individualCategory;
    private Category tazkiaValueCategory;

    public DaftarBawahanController() {
        individualCategory = new Category();
        individualCategory.setId(CategoryConstants.INDIVIDUAL_INDICATOR_ID);

        tazkiaValueCategory = new Category();
        tazkiaValueCategory.setId(CategoryConstants.TAZKIA_INDICATOR_ID);
    }


    @GetMapping("/daftarbawahan/list")
    public String daftarStaff(Model model, String user, Authentication currentUser,
                              @PageableDefault(size = 10)
                                      Pageable page, String search) throws Exception {
        System.out.println("username" + currentUser.getClass().getName());

        if (currentUser == null) {
            LOGGER.warn("Current user is null");
            return "redirect:/404";
        }

        String username = ((UserDetails) currentUser.getPrincipal()).getUsername();
        User u = userDao.findByUsername(username);

        if (u == null) {
            LOGGER.warn("Username {} not found in database " + username);
            return "redirect:/404";
        }

        Staff p = staffDao.findByUser(u);


        if (p == null) {
            LOGGER.warn("Staff not found for username {} " + username);
            return "redirect:/404";
        }

        Staff atasan = staffDao.findOne(p.getId());
        Page<Staff> daftarBawahan = staffDao.findSubordinate(atasan, page);

        if (StringUtils.hasText(search)) {
            model.addAttribute("search", search);
            model.addAttribute("subordinate", staffDao.findBySuperiorsAndEmployeeNameContainingIgnoreCaseOrderByEmployeeName(atasan, search, page));
        } else {
            model.addAttribute("subordinate", daftarBawahan);
        }

        return "/daftarbawahan/list";

    }

    @GetMapping("/daftarbawahan/form")
    public String daftarKpi(String id, Model m, Pageable page, Authentication currentUser) {
        Staff s = staffDao.findOne(id);

        if (s == null) {
            LOGGER.warn("Staff not found");
            return "redirect:/404";
        }

        m.addAttribute("staff", s);

        System.out.println("username" + currentUser.getClass().getName());

        if (currentUser == null) {
            LOGGER.warn("Current user is null");
            return "redirect:/404";
        }

        String username = ((UserDetails) currentUser.getPrincipal()).getUsername();
        User u = userDao.findByUsername(username);

        if (u == null) {
            LOGGER.warn("Username {} not found in database " + username);
            return "redirect:/404";
        }


        m.addAttribute("individual", staffKpiDao.findAllByStaffAndKpiCategory(s, individualCategory));
        m.addAttribute("tazkiaValue", staffKpiDao.findAllByStaffAndKpiCategory(s, tazkiaValueCategory));

        return "/daftarbawahan/form";

    }

    @ModelAttribute("daftarIndicators")
    public Iterable<Indicators> daftarIndicators() {
        return indicatorsDao.findAll();
    }


    @PostMapping(value = "/daftarbawahan/form")
    public String prosesForm(@RequestParam String staff, HttpServletRequest request) {
        Staff s = staffDao.findOne(staff);
        System.out.println("Staff : " + s.getEmployeeName());

        List<StaffKpi> daftarKpi = staffKpiDao.findAllByStaffAndKpiCategory(s, tazkiaValueCategory);
        List<StaffKpi> daftarKpiIndividual = staffKpiDao.findAllByStaffAndKpiCategory(s, individualCategory);

        for (StaffKpi skp : daftarKpi) {
            String pilihan = request.getParameter(skp.getKpi().getId() + "-score");
            System.out.println("Pilihan : " + pilihan);
            Indicators indicators = indicatorsDao.findOne(pilihan);
            if (indicators != null) {
                Score score = new Score();
                score.setStaffKpi(skp);
                score.setScore(indicators.getScore());
                score.setRemark(indicators.getContent());
                float val = Float.parseFloat(score.getStaffKpi().getKpi().getWeight());
                float scoree = Float.parseFloat(score.getScore());
                LOGGER.info("perkalian 1         " + val);
                LOGGER.info(" perkalian 2          " + scoree);
                LOGGER.info("hasilnya         " + val * scoree);
                float perkalian = val * scoree;
                String hasile = String.valueOf(perkalian);
                score.setTotal(hasile);
                scoreDao.save(score);
            }

        }

        for (StaffKpi sk : daftarKpiIndividual) {
            String pilihan = request.getParameter(sk.getKpi().getId() + "-score");

            System.out.println("Pilihan : " + pilihan);
            Indicators indicators = indicatorsDao.findOne(pilihan);
            if (indicators != null) {
                Score score = new Score();
                score.setStaffKpi(sk);
                score.setScore(indicators.getScore());
                score.setRemark(indicators.getContent());
                float val = Float.parseFloat(score.getStaffKpi().getKpi().getWeight());
                float scoree = Float.parseFloat(score.getScore());
                LOGGER.info("perkalian 1         " + val);
                LOGGER.info(" perkalian 2          " + scoree);
                LOGGER.info("hasilnya         " + val * scoree);
                float perkalian = val * scoree;
                String hasile = String.valueOf(perkalian);
                score.setTotal(hasile);
                scoreDao.save(score);
            }


        }
        return "redirect:list";
    }


    @GetMapping("/daftarbawahan/detail")
    public String detail(@RequestParam(required = false) String id, Model m, Pageable page, Authentication currentUser) {
        Staff s = staffDao.findOne(id);
        if (s == null) {
            LOGGER.warn("Staff not found");
            return "redirect:/404";
        }


        System.out.println("username" + currentUser.getClass().getName());

        if (currentUser == null) {
            LOGGER.warn("Current user is null");
            return "redirect:/404";
        }

        String username = ((UserDetails) currentUser.getPrincipal()).getUsername();
        User u = userDao.findByUsername(username);

        if (u == null) {
            LOGGER.warn("Username {} not found in database " + username);
            return "redirect:/404";
        }

        Staff p = staffDao.findByUser(u);

        Staff atasan = staffDao.findOne(p.getId());

        if (StringUtils.hasText(id)) {
            m.addAttribute("nama", id);
            m.addAttribute("detail", staffDao.findOne(id));
            m.addAttribute("superior", atasan);
        } else {
            m.addAttribute("detailStaff", staffDao.findAll(page));
        }


        return "/daftarbawahan/detail";

    }

    @GetMapping("/daftarbawahan/komen")
    public String String(@RequestParam(required = true) String id, Model m, Authentication currentUser) throws Exception {
        Staff s = staffDao.findOne(id);

        if (s == null) {
            LOGGER.warn("Staff not found");
            return "redirect:/404";
        }

        System.out.println("username" + currentUser.getClass().getName());

        if (currentUser == null) {
            LOGGER.warn("Current user is null");
            return "redirect:/404";
        }

        String username = ((UserDetails) currentUser.getPrincipal()).getUsername();
        User u = userDao.findByUsername(username);

        if (u == null) {
            LOGGER.warn("Username {} not found in database " + username);
            return "redirect:/404";
        }

        Staff p = staffDao.findByUser(u);


        m.addAttribute("individual", scoreDao.findByStaffKpiStaffIdAndStaffKpiKpiCategoryOrderByStaffKpiAsc(id, individualCategory));
        m.addAttribute("tazkiaValue", scoreDao.findByStaffKpiStaffIdAndStaffKpiKpiCategoryOrderByStaffKpiAsc(id, tazkiaValueCategory));


        return "/daftarbawahan/komen";
    }

    @GetMapping("/uploaded/{evidence}/bukti/")
    public ResponseEntity<byte[]> tampilkanBuktiPembayaran(@PathVariable Evidence evidence) throws Exception {
        String lokasiFile = uploadFolder + File.separator + evidence.getScore().getStaffKpi().getStaff().getId()
                + File.separator + evidence.getFileName();
        LOGGER.debug("Lokasi file bukti : {}", lokasiFile);

        try {
            HttpHeaders headers = new HttpHeaders();
            if (evidence.getFileName().toLowerCase().endsWith("jpeg") || evidence.getFileName().toLowerCase().endsWith("jpg")) {
                headers.setContentType(MediaType.IMAGE_JPEG);
            } else if (evidence.getFileName().toLowerCase().endsWith("png")) {
                headers.setContentType(MediaType.IMAGE_PNG);
            } else if (evidence.getFileName().toLowerCase().endsWith("pdf")) {
                headers.setContentType(MediaType.APPLICATION_PDF);
            } else {
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            }
            byte[] data = Files.readAllBytes(Paths.get(lokasiFile));
            return new ResponseEntity<byte[]>(data, headers, HttpStatus.OK);
        } catch (Exception err) {
            LOGGER.warn(err.getMessage(), err);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
