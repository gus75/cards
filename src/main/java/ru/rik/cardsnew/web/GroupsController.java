package ru.rik.cardsnew.web;

import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ru.rik.cardsnew.db.BoxRepo;
import ru.rik.cardsnew.db.CardRepo;
import ru.rik.cardsnew.db.ChannelRepo;
import ru.rik.cardsnew.db.GroupRepo;
import ru.rik.cardsnew.db.TrunkRepo;
import ru.rik.cardsnew.domain.Grp;
import ru.rik.cardsnew.domain.Oper;

@Controller
@RequestMapping("/groups")
@EnableTransactionManagement
@SessionAttributes("filter") 
public class GroupsController {
	
	@Autowired GroupRepo groups;
	@Autowired BoxRepo boxes;
	@Autowired TrunkRepo trunks;
	@Autowired ChannelRepo chans;
	@Autowired CardRepo cards;
	@Autowired Filter filter;
	
	public GroupsController() { 
		super();
	}
	
	@Transactional
	@RequestMapping(method = RequestMethod.GET)
	public String spittles(Model model) {
		List<Grp> list = groups.findAll();
//		if (!model.containsAttribute("cardFilter")) {
//			Filter cf = new Filter();
//			model.addAttribute("cardFilter", cf);
//			logger.debug("create cardFilter attribute");
//		}
		
		model.addAttribute("grps", list);
		return "groups";
	}

	@RequestMapping(value="/add", method=RequestMethod.GET)
	public String  addEntity(Model model) {
		Grp group = new Grp();
		model.addAttribute("group", group);
		return "group-edit";
	}

	
	@Transactional
	@RequestMapping(value="/edit", method=RequestMethod.GET)
	public String  editPage(@RequestParam(value="id", required=true) long id, Model model) {
		filter.setId(id);
		if(! model.containsAttribute("group")) {
			Grp group = groups.findById(id);
//			group.getCards().size();
//			group.getChannels().size();
			addToModel(model, group);	
			model.addAttribute("filter", filter);
		}
		return "group-edit";
	}
	
	private void addToModel(Model model, Grp group) {
		model.addAttribute("group", group);
		model.addAttribute("opers", Oper.values()); 
	}
	
	@Transactional
	@RequestMapping(value="/edit", method=RequestMethod.POST)
	public String editEntity(
			@Valid @ModelAttribute Grp group, 
			BindingResult result,
			Model model,  
			RedirectAttributes redirectAttrs,
			@RequestParam(value="action", required=true) String action ) {
		
		if (action.equals("cancel")) {
			String message = group.toString() + " edit cancelled";
			redirectAttrs.addFlashAttribute("message", message);
			
		} else if (result.hasErrors()) {
			System.out.println("there are validation errors" + result.getAllErrors().toString());
			redirectAttrs.addFlashAttribute("org.springframework.validation.BindingResult.channel", result);
			redirectAttrs.addFlashAttribute("group", group);
			
			return "redirect:/groups/edit?id=" + group.getId();
			
		} else if (action.equals("save") && group != null) {
			groups.makePersistent(group);
		} 

		return "redirect:/groups";		
	}
	
	@Transactional
	@RequestMapping(value = "/delete", method = RequestMethod.GET)
	public String delete (Model model,
			RedirectAttributes redirectAttrs,
			@RequestParam(value = "id", required = true) long id,
			@RequestParam(value="phase", required=true) String phase) {
		
		Grp group = groups.findById(id);
		String view = null;
		
		if (phase.equals("confirm")) {
			view ="redirect:/groups";
			groups.makeTransient(group);
			
			String message = "Channel " + group.getName() + " was successfully deleted";
			redirectAttrs.addFlashAttribute("message", message);
		}
		
		return view;
	}
	

}
