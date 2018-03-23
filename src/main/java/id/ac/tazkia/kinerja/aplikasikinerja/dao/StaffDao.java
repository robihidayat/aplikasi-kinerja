package id.ac.tazkia.kinerja.aplikasikinerja.dao;

import id.ac.tazkia.kinerja.aplikasikinerja.entity.Staff;
import id.ac.tazkia.kinerja.aplikasikinerja.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface StaffDao extends PagingAndSortingRepository<Staff, String> {
    Staff findByUser(User u);

    @Query("select s from Staff s where :superior member of s.superiors")
    List<Staff> test(@Param("superior") Staff superior);
}
