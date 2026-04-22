package com.mayuhao.demo1.controller;

import com.mayuhao.demo1.dto.TicketCreateRequest;
import com.mayuhao.demo1.dto.TicketResponse;
import com.mayuhao.demo1.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public TicketResponse create(@Valid @RequestBody TicketCreateRequest request) {
        return ticketService.createTicket(request);
    }

    @GetMapping
    public List<TicketResponse> list() {
        return ticketService.listTickets();
    }

    @GetMapping("/{id}")
    public TicketResponse getById(@PathVariable Long id) {
        return ticketService.getTicket(id);
    }
}
