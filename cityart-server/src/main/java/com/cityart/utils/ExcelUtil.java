package com.cityart.utils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Excel 导出工具（基于 POI）
 * <p>
 * ⚠️ 依赖的是 POI 3.16，用的是这版就有的 API
 * （3.16 没有 3.17+ 的 {@code CellStyle#setFillForegroundColor(IndexedColors)} 之外的一些新方法，
 * 也不要用 4.x 才有的 {@code Workbook#createCellStyle} 之外的写法）。
 */
public class ExcelUtil {

    private ExcelUtil() {
    }

    /**
     * 把二维数据写成 xlsx 字节数组
     *
     * @param sheetName 工作表名
     * @param headers   表头（第一行）
     * @param rows      数据行；每个元素是一行的各列值（按表头顺序）
     * @return 可直接作为响应体写出的 xlsx 字节
     */
    public static byte[] write(String sheetName, String[] headers, List<String[]> rows) {
        Workbook workbook = new XSSFWorkbook();
        try {
            Sheet sheet = workbook.createSheet(sheetName == null ? "Sheet1" : sheetName);

            // 表头样式：加粗 + 灰底
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);

            // ① 表头
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // ② 数据行
            if (rows != null) {
                for (int r = 0; r < rows.size(); r++) {
                    Row row = sheet.createRow(r + 1);
                    String[] values = rows.get(r);
                    for (int c = 0; c < headers.length; c++) {
                        String value = values != null && c < values.length ? values[c] : null;
                        row.createCell(c).setCellValue(value == null ? "" : value);
                    }
                }
            }

            // ③ 自动列宽（要在数据写完后调用，否则量不准）
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                // autoSizeColumn 对中文量不准，手工再留一点余量
                int width = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.min((int) (width * 1.6), 255 * 256));
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("生成 Excel 失败", e);
        } finally {
            try {
                workbook.close();
            } catch (IOException ignored) {
                // 关闭失败不影响已生成的字节流
            }
        }
    }
}
