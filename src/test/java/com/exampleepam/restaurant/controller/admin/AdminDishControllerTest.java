package com.exampleepam.restaurant.controller.admin;

import com.exampleepam.restaurant.ControllerConfiguration;
import com.exampleepam.restaurant.dto.dish.DishCreationDto;
import com.exampleepam.restaurant.security.MyUserDetailsService;
import com.exampleepam.restaurant.service.DishService;
import com.exampleepam.restaurant.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;

import static com.exampleepam.restaurant.test_data.TestData.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminDishController.class)
@Import(ControllerConfiguration.class)
@WithUserDetails(ADMIN_EMAIL)
public class AdminDishControllerTest {
    MockMvc mockMvc;


    @MockBean
    private UserService userService;
    @MockBean
    private DishService dishService;
    @MockBean
    MyUserDetailsService myUserDetailsService;

    @Autowired
    public AdminDishControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void findPaginated() throws Exception {
        String sortField = "category";
        String sortDir = "asc";
        String category = "active";
        int pageSize = 5;
        int pageNo = 25;
        var mockRequest =
                MockMvcRequestBuilders.get("/admin/dishes/page/25")
                        .param("sortField", sortField)
                        .param("sortDir", sortDir)
                        .param("filterCategory", category)
                        .param("pageSize", String.valueOf(pageSize))
                        .with(csrf());

        Mockito.when(dishService.findPaginated(pageNo, pageSize, sortField, sortDir,
                        category))
                .thenReturn(getDishResponseDtosPaged());
        Mockito.when(userService.getUserBalance(2)).thenReturn(BigDecimal.valueOf(2321));

        mockMvc.perform(mockRequest)
                .andExpect(status().isOk())
                .andExpect(view().name("dishes-management"))
                .andExpect(model().attributeExists("sortDir",
                        "filterCategory", "pageSize", "currentPage", "reverseSortDir",
                        "dishPaged"));
    }

    @Test
    void returnDishCreationForm() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/dishes/newDishForm"))
                .andExpect(view().name("dish-add"))
                .andExpect(model().attributeExists("dish"));
    }

    @Test
    void saveNewDish() throws Exception {
        DishCreationDto dishCreationDto = getDishCreationDto();


        MockMultipartFile file1 = new MockMultipartFile(
                "images",
                "image1.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "file1".getBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
                "images",
                "image2.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "file2".getBytes()
        );
        mockMvc.perform(multipart("/admin/dishes")
                        .file(file1)
                        .file(file2)
                        .param("primaryIndex", "0")
                        .flashAttr("dish", dishCreationDto)
                        .with(csrf()))
                .andExpect(redirectedUrl("/admin/dishes"));
        Mockito.verify(dishService, Mockito.times(1)).saveWithFiles(Mockito.eq(dishCreationDto), Mockito.anyList());
    }

    @Test
    void saveNewDishValidationFail() throws Exception {
        DishCreationDto invalidDto = new DishCreationDto();
        MockMultipartFile emptyFile = new MockMultipartFile(
                "images",
                "image.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[0]
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/admin/dishes")
                        .file(emptyFile)
                        .flashAttr("dish", invalidDto)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("dish-add"))
                .andExpect(model().attributeExists("categories"));

        Mockito.verify(dishService, Mockito.never()).saveWithFiles(Mockito.any(), Mockito.anyList());
    }

    @Test
    void deleteDish() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/admin/dishes/10/page/5")
                        .param("sortField", "category")
                        .param("sortDir", "asc")
                        .param("filterCategory", "active")
                        .param("pageSize", "10")
                        .with(csrf()))
                .andExpect(status()
                        .is3xxRedirection());
        Mockito.verify(dishService, Mockito.times(1)).deleteDishById(10);
    }

    @Test
    void restoreDish() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/admin/dishes/15/restore/page/2")
                        .param("sortField", "name")
                        .param("sortDir", "asc")
                        .param("filterCategory", "archived")
                        .param("pageSize", "10")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
        Mockito.verify(dishService, Mockito.times(1)).restoreDishById(15);
    }

    @Test
    void hardDeleteDish() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/admin/dishes/20/hard-delete/page/1")
                        .param("sortField", "name")
                        .param("sortDir", "asc")
                        .param("filterCategory", "archived")
                        .param("pageSize", "10")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
        Mockito.verify(dishService, Mockito.times(1)).hardDeleteDish(20);
    }

    @Test
    void returnDishUpdateForm() throws Exception {
        Mockito.when(dishService.getDishById(5)).thenReturn(getDishResponseDto());


        mockMvc.perform(MockMvcRequestBuilders.get("/admin/dishes/5/update-form")

                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("dish-update"))
                .andExpect(model().attributeExists("dish"));
        Mockito.verify(dishService, Mockito.times(1)).getDishById(5);

    }


    @Test
    void updateDish() throws Exception {
        MockMultipartHttpServletRequestBuilder putMultipart = (MockMultipartHttpServletRequestBuilder)
                MockMvcRequestBuilders.multipart("/admin/dishes/3")
                        .with(rq -> {
                            rq.setMethod("PUT");
                            return rq;
                        });
        DishCreationDto dishCreationDto = getDishCreationDto();

        MockMultipartFile emptyFile = new MockMultipartFile(
                "images",
                "image.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[0]
        );

        mockMvc.perform(putMultipart
                        .file(emptyFile)
                        .param("existingImages", "image.jpg")
                        .param("primaryIndex", "0")
                        .flashAttr("dish", dishCreationDto)
                        .with(csrf()))
                .andExpect(redirectedUrl("/admin/dishes"));
        Mockito.verify(dishService, Mockito.times(1)).updateWithFiles(Mockito.eq(dishCreationDto), Mockito.anyList(), Mockito.anyMap(), Mockito.any());
    }

}
