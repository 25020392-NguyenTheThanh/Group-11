package com.example.group11.controller;

import javafx.scene.control.DatePicker;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

import java.io.File;
import java.time.LocalDate;

import static com.example.group11.controller.NotificationController.showAlert;

/**
 * Lớp kiểm tra tính hợp lệ dữ liệu đăng ký sản phẩm (Validator).
 * Thực hiện các nghiệp vụ kiểm tra thông tin nhập vào từ form đăng ký và cập nhật sản phẩm.
 */
public class ProductRegistrationValidator {

    /**
     * Thực hiện kiểm tra toàn diện tất cả các điều kiện hợp lệ khi đăng ký sản phẩm mới.
     * Bao gồm: kiểm tra bỏ trống, kiểm tra định dạng số và kiểm tra tính hợp lệ thời gian.
     *
     * @param productNameField Ô nhập tên sản phẩm
     * @param categoryMenuButton Nút chọn danh mục sản phẩm
     * @param startingPriceField Ô nhập giá khởi điểm
     * @param minimumBidIncrementField Ô nhập bước giá tối thiểu
     * @param startDatePicker Trình chọn ngày bắt đầu đấu giá
     * @param endDatePicker Trình chọn ngày kết thúc đấu giá
     * @param descriptionArea Vùng nhập mô tả sản phẩm
     * @param selectedImageFile Tệp tin hình ảnh đã chọn
     * @param imageDropzone Vùng kéo thả hình ảnh
     * @return true nếu toàn bộ thông tin hợp lệ, ngược lại trả về false
     */
    public static boolean validateAll (TextField productNameField, MenuButton categoryMenuButton, TextField startingPriceField,
                                       TextField minimumBidIncrementField, DatePicker startDatePicker, DatePicker endDatePicker,
                                       TextArea descriptionArea, File selectedImageFile, StackPane imageDropzone) {
        return validateEmptyFields(productNameField, categoryMenuButton, startingPriceField, minimumBidIncrementField, startDatePicker, endDatePicker,
                descriptionArea, selectedImageFile, imageDropzone) && validateNumericFormats(startingPriceField, minimumBidIncrementField) && validateDateTime(
                        startDatePicker, endDatePicker
        );
    }

    /**
     * Kiểm tra xem các trường nhập liệu bắt buộc có bị bỏ trống hay không.
     *
     * @param productNameField Ô nhập tên sản phẩm
     * @param categoryMenuButton Nút chọn danh mục sản phẩm
     * @param startingPriceField Ô nhập giá khởi điểm
     * @param minimumBidIncrementField Ô nhập bước giá tối thiểu
     * @param startDatePicker Trình chọn ngày bắt đầu đấu giá
     * @param endDatePicker Trình chọn ngày kết thúc đấu giá
     * @param descriptionArea Vùng nhập mô tả sản phẩm
     * @param selectedImageFile Tệp tin hình ảnh đã chọn
     * @param imageDropzone Vùng kéo thả hình ảnh
     * @return true nếu tất cả các trường được điền đầy đủ, ngược lại trả về false
     */
    public static boolean validateEmptyFields(TextField productNameField, MenuButton categoryMenuButton, TextField startingPriceField,
                                        TextField minimumBidIncrementField, DatePicker startDatePicker, DatePicker endDatePicker,
                                        TextArea descriptionArea, File selectedImageFile, StackPane imageDropzone) {
        // Kiểm tra tên
        if (productNameField.getText().trim().isEmpty()) {
            showAlert("Lỗi nhập liệu", "Tên sản phẩm không được để trống!");
            productNameField.requestFocus();
            return false;
        }

        // Kiểm tra danh mục
        String category = categoryMenuButton.getText();
        if (category == null || category.isEmpty() || category.equals("Chọn danh mục")) {
            showAlert("Lỗi nhập liệu", "Vui lòng chọn danh mục cho sản phẩm!");
            categoryMenuButton.requestFocus();
            return false;
        }

        // Kiểm tra chuỗi nhập giá khởi điểm
        if (startingPriceField.getText().trim().isEmpty()) {
            showAlert("Lỗi nhập liệu", "Giá khởi điểm không được để trống!");
            startingPriceField.requestFocus();
            return false;
        }

        // Kiểm tra chuỗi nhập bước giá
        if (minimumBidIncrementField.getText().trim().isEmpty()) {
            showAlert("Lỗi nhập liệu", "Bước giá tối thiểu không được để trống!");
            minimumBidIncrementField.requestFocus();
            return false;
        }

        // Kiểm tra ngày bắt đầu/kết thúc
        if (startDatePicker.getValue() == null) {
            showAlert("Lỗi nhập liệu", "Vui lòng chọn ngày bắt đầu đấu giá!");
            startDatePicker.requestFocus();
            return false;
        }
        if (endDatePicker.getValue() == null) {
            showAlert("Lỗi nhập liệu", "Vui lòng chọn ngày kết thúc đấu giá!");
            endDatePicker.requestFocus();
            return false;
        }

        // Kiểm tra mô tả
        if (descriptionArea.getText().trim().isEmpty()) {
            showAlert("Lỗi nhập liệu", "Mô tả sản phẩm không được để trống!");
            descriptionArea.requestFocus();
            return false;
        }

        // Kiểm tra ảnh
        if (selectedImageFile == null) {
            showAlert("Lỗi thiếu thông tin", "Vui lòng chọn hình ảnh đại diện cho sản phẩm!");
            imageDropzone.requestFocus();
            return false;
        }

        return true; // Tất cả đều đã điền
    }


    /**
     * Kiểm tra định dạng số và lô-gíc của giá khởi điểm và bước giá tối thiểu.
     * Đảm bảo các giá trị lớn hơn 0 và bước giá không vượt quá giá khởi điểm.
     *
     * @param startingPriceField Ô nhập giá khởi điểm
     * @param minimumBidIncrementField Ô nhập bước giá tối thiểu
     * @return true nếu định dạng và logic số hợp lệ, ngược lại trả về false
     */
    public static boolean validateNumericFormats(TextField startingPriceField,
                                           TextField minimumBidIncrementField) {
        double startingPrice;
        double bidIncrement;

        // 1. Ép kiểu giá khởi điểm
        try {
            startingPrice = Double.parseDouble(startingPriceField.getText().trim());
            if (startingPrice <= 0) {
                showAlert("Lỗi định dạng", "Giá khởi điểm phải là một số lớn hơn 0!");
                startingPriceField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("Lỗi định dạng", "Giá khởi điểm phải là một số hợp lệ (Ví dụ: 100000)!");
            startingPriceField.requestFocus();
            return false;
        }

        // 2. Ép kiểu bước giá tối thiểu
        try {
            bidIncrement = Double.parseDouble(minimumBidIncrementField.getText().trim());
            if (bidIncrement <= 0) {
                showAlert("Lỗi định dạng", "Bước giá tối thiểu phải lớn hơn 0!");
                minimumBidIncrementField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("Lỗi định dạng", "Bước giá tối thiểu phải là một số hợp lệ!");
            minimumBidIncrementField.requestFocus();
            return false;
        }

        // 3. Logic so sánh giữa 2 số
        if (bidIncrement > startingPrice) {
            showAlert("Lỗi logic", "Bước giá tối thiểu không được lớn hơn giá khởi điểm!");
            minimumBidIncrementField.requestFocus();
            return false;
        }

        return true; // Định dạng số hoàn toàn hợp lệ
    }

    /**
     * Kiểm tra logic thời gian đấu giá.
     * Đảm bảo ngày bắt đầu không ở quá khứ và ngày kết thúc phải sau ngày bắt đầu ít nhất 1 ngày.
     *
     * @param startDatePicker Trình chọn ngày bắt đầu đấu giá
     * @param endDatePicker Trình chọn ngày kết thúc đấu giá
     * @return true nếu logic thời gian hợp lệ, ngược lại trả về false
     */
    public static boolean validateDateTime(DatePicker startDatePicker, DatePicker endDatePicker) {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        // Ngày bắt đầu không được ở quá khứ
        if (startDate.isBefore(LocalDate.now())) {
            showAlert("Lỗi thời gian", "Ngày bắt đầu không được ở trong quá khứ!");
            startDatePicker.requestFocus();
            return false;
        }

        // Ngày kết thúc phải sau ngày bắt đầu
        if (!endDate.isAfter(startDate)) {
            showAlert("Lỗi thời gian", "Ngày kết thúc phải sau ngày bắt đầu ít nhất 1 ngày!");
            endDatePicker.requestFocus();
            return false;
        }

        return true; // Thời gian hoàn toàn hợp lệ
    }

    /**
     * Thực hiện kiểm tra tính hợp lệ khi cập nhật (sửa) sản phẩm hiện tại.
     * Cho phép giữ nguyên hình ảnh cũ nếu không tải lên hình ảnh mới.
     *
     * @param productNameField Ô nhập tên sản phẩm
     * @param categoryMenuButton Nút chọn danh mục sản phẩm
     * @param startingPriceField Ô nhập giá khởi điểm
     * @param descriptionArea Vùng nhập mô tả sản phẩm
     * @param hasExistingImage Trạng thái sản phẩm hiện tại đã có ảnh lưu trên hệ thống hay chưa
     * @param selectedImageFile Tệp tin hình ảnh mới đã chọn
     * @param imageDropzone Vùng kéo thả hình ảnh
     * @return true nếu thông tin chỉnh sửa hợp lệ, ngược lại trả về false
     */
    public static boolean validateEdit(TextField productNameField, MenuButton categoryMenuButton, TextField startingPriceField,
                                       TextArea descriptionArea, boolean hasExistingImage, File selectedImageFile, StackPane imageDropzone) {
        // Kiểm tra tên
        if (productNameField.getText().trim().isEmpty()) {
            showAlert("Lỗi nhập liệu", "Tên sản phẩm không được để trống!");
            productNameField.requestFocus();
            return false;
        }

        // Kiểm tra danh mục
        String category = categoryMenuButton.getText();
        if (category == null || category.isEmpty() || category.equals("Chọn danh mục")) {
            showAlert("Lỗi nhập liệu", "Vui lòng chọn danh mục cho sản phẩm!");
            categoryMenuButton.requestFocus();
            return false;
        }

        // Kiểm tra chuỗi nhập giá khởi điểm
        if (startingPriceField.getText().trim().isEmpty()) {
            showAlert("Lỗi nhập liệu", "Giá khởi điểm không được để trống!");
            startingPriceField.requestFocus();
            return false;
        }

        double startingPrice;
        try {
            startingPrice = Double.parseDouble(startingPriceField.getText().trim());
            if (startingPrice <= 0) {
                showAlert("Lỗi định dạng", "Giá khởi điểm phải là một số lớn hơn 0!");
                startingPriceField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("Lỗi định dạng", "Giá khởi điểm phải là một số hợp lệ (Ví dụ: 100000)!");
            startingPriceField.requestFocus();
            return false;
        }

        // Kiểm tra mô tả
        if (descriptionArea.getText().trim().isEmpty()) {
            showAlert("Lỗi nhập liệu", "Mô tả sản phẩm không được để trống!");
            descriptionArea.requestFocus();
            return false;
        }

        // Kiểm tra ảnh: chỉ báo lỗi nếu cả ảnh mới lẫn ảnh cũ đều không có
        if (selectedImageFile == null && !hasExistingImage) {
            showAlert("Lỗi thiếu thông tin", "Vui lòng chọn hình ảnh đại diện cho sản phẩm!");
            imageDropzone.requestFocus();
            return false;
        }

        return true;
    }
}
