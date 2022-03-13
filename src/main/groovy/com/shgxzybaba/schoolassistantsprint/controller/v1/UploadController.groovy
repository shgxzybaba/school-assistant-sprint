package com.shgxzybaba.schoolassistantsprint.controller.v1

import builders.dsl.spreadsheet.query.api.SpreadsheetCriteria
import builders.dsl.spreadsheet.query.poi.PoiSpreadsheetCriteria
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.transform.TupleConstructor
import groovy.transform.builder.Builder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile


import java.text.DateFormat
import java.text.SimpleDateFormat

@RestController
@RequestMapping('/v1/upload/')
class UploadController {
    @Autowired
    UploadService uploadService




    @PostMapping('/students/single')
    ResponseEntity<Void> addSingleStudent(@RequestBody StudentModel model) {
        uploadService.addSingleStudent(model)
        ResponseEntity.accepted().build()
    }

    @PostMapping('/students/file')
    ResponseEntity<Void> addStudentInBulk(@RequestParam("file") MultipartFile file) {
        uploadService.addStudentsWithFile(file)
        ResponseEntity.accepted().build()
    }

}

@Service
class UploadService {
    private List<StudentModel> students = []
    static DateFormat df = new SimpleDateFormat("dd-MM-yyyy")

    UUID addSingleStudent(StudentModel model) {
        model.generateId()
        students << model
        model.id
    }

    void addStudentsWithFile(MultipartFile file) {
        def studentsFromFile = []
        File  f = new File('/Users/akindurooluwasegun/Documents/personal/school-assistant-sprint/model.xlsx')
        f.mkdir()
        try (OutputStream outStream = new FileOutputStream(f)) {
            outStream.write(file.getBytes())
        }

        SpreadsheetCriteria query = PoiSpreadsheetCriteria.FACTORY.forFile(f)

        def rows = query.query {
            it.sheet("Students") {sheetQuery ->
                sheetQuery.row {rowQuery ->
                    rowQuery.cell(1){
                        it.having{cell -> cell.value != null && cell.value != ''}
                    }
                }
            }
        }.rows




        rows.each {

            Date dob
            try {
                def month = (it.cells[4].value as Integer).toString().length() < 2 ? "0${(it.cells[4].value as Integer).toString()}" : (it.cells[4].value as Integer).toString()
                dob = df.parse("${it.cells[3].value as Integer}-$month-${it.cells[5].value}")
            } catch (NullPointerException | IllegalArgumentException e) {
                throw new IllegalArgumentException('One or more column values are missing!')
            }

            StudentModel model = new StudentModel(
                    name: it.cells[0].value as String,
                    age : it.cells[1].value as Integer,
                    classArm: it.cells[2].value as Character,
                    dateOfBirth: dob
            )

            model.generateId()
            studentsFromFile << model
        }

        students.addAll(studentsFromFile)

    }

}

@Builder
@TupleConstructor
@ToString
@EqualsAndHashCode(excludes = ["id","classArm"])
class StudentModel {

    String name
    int age
    char classArm
    Date dateOfBirth

    UUID id

    void generateId() {
        id = UUID.randomUUID()
    }



}
