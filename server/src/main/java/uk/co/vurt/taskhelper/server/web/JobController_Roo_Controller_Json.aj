// WARNING: DO NOT EDIT THIS FILE. THIS FILE IS MANAGED BY SPRING ROO.
// You may push code into the target .java compilation unit if you wish to edit any member(s).

package uk.co.vurt.taskhelper.server.web;

import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import uk.co.vurt.taskhelper.server.domain.job.Job;
import uk.co.vurt.taskhelper.server.domain.job.Status;
import uk.co.vurt.taskhelper.server.domain.user.Person;

privileged aspect JobController_Roo_Controller_Json {
    
    @RequestMapping(value = "/{id}", headers = "Accept=application/json")
    @ResponseBody
    public ResponseEntity<java.lang.String> JobController.showJson(@PathVariable("id") java.lang.Long id) {
        Job job = Job.findJob(id);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/text; charset=utf-8");
        if (job == null) {
            return new ResponseEntity<String>(headers, HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<String>(job.jsonify(), headers, HttpStatus.OK);
    }
    
    @RequestMapping(headers = "Accept=application/json")
    @ResponseBody
    public ResponseEntity<java.lang.String> JobController.listJson() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/text; charset=utf-8");
        List<Job> result = Job.findAllJobs();
        return new ResponseEntity<String>(Job.jsonifyArray(result), headers, HttpStatus.OK);
    }
    
    @RequestMapping(method = RequestMethod.POST, headers = "Accept=application/json")
    public ResponseEntity<java.lang.String> JobController.createFromJson(@RequestBody java.lang.String json) {
        Job job = Job.fromJsonToJob(json);
        job.persist();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/text");
        return new ResponseEntity<String>(headers, HttpStatus.CREATED);
    }
    
    @RequestMapping(value = "/jsonArray", method = RequestMethod.POST, headers = "Accept=application/json")
    public ResponseEntity<java.lang.String> JobController.createFromJsonArray(@RequestBody java.lang.String json) {
        for (Job job: Job.fromJsonArrayToJobs(json)) {
            job.persist();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/text");
        return new ResponseEntity<String>(headers, HttpStatus.CREATED);
    }
    
    @RequestMapping(method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity<java.lang.String> JobController.updateFromJson(@RequestBody java.lang.String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/text");
        Job job = Job.fromJsonToJob(json);
        if (job.merge() == null) {
            return new ResponseEntity<String>(headers, HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<String>(headers, HttpStatus.OK);
    }
    
    @RequestMapping(value = "/jsonArray", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity<java.lang.String> JobController.updateFromJsonArray(@RequestBody java.lang.String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/text");
        for (Job job: Job.fromJsonArrayToJobs(json)) {
            if (job.merge() == null) {
                return new ResponseEntity<String>(headers, HttpStatus.NOT_FOUND);
            }
        }
        return new ResponseEntity<String>(headers, HttpStatus.OK);
    }
    
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, headers = "Accept=application/json")
    public ResponseEntity<java.lang.String> JobController.deleteFromJson(@PathVariable("id") java.lang.Long id) {
        Job job = Job.findJob(id);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/text");
        if (job == null) {
            return new ResponseEntity<String>(headers, HttpStatus.NOT_FOUND);
        }
        job.remove();
        return new ResponseEntity<String>(headers, HttpStatus.OK);
    }
    
    @RequestMapping(params = "find=ByPerson", headers = "Accept=application/json")
    @ResponseBody
    public ResponseEntity<java.lang.String> JobController.jsonFindJobsByPerson(@RequestParam("person") Person person) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/text; charset=utf-8");
        return new ResponseEntity<String>(Job.jsonifyArray(Job.findJobsByPerson(person).getResultList()), headers, HttpStatus.OK);
    }
    
    @RequestMapping(params = "find=ByStatus", headers = "Accept=application/json")
    @ResponseBody
    public ResponseEntity<java.lang.String> JobController.jsonFindJobsByStatus(@RequestParam("status") Status status) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/text; charset=utf-8");
        return new ResponseEntity<String>(Job.jsonifyArray(Job.findJobsByStatus(status).getResultList()), headers, HttpStatus.OK);
    }
    
}
