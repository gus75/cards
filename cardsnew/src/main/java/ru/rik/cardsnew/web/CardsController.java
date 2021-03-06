package ru.rik.cardsnew.web;

import java.util.ArrayList;
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

import ru.rik.cardsnew.db.BankRepo;
import ru.rik.cardsnew.db.CardRepo;
import ru.rik.cardsnew.db.ChannelRepo;
import ru.rik.cardsnew.db.GroupRepo;
import ru.rik.cardsnew.db.TrunkRepo;
import ru.rik.cardsnew.domain.Bank;
import ru.rik.cardsnew.domain.Card;
import ru.rik.cardsnew.domain.CardStat;
import ru.rik.cardsnew.domain.Grp;
import ru.rik.cardsnew.domain.Oper;
import ru.rik.cardsnew.domain.Place;

@Controller
@RequestMapping("/cards")
@EnableTransactionManagement
@SessionAttributes("filter") 
public class CardsController {

	@Autowired GroupRepo groups;
	@Autowired BankRepo banks;
	@Autowired CardRepo cards;
	@Autowired ChannelRepo channels;
	@Autowired TrunkRepo trunks;
	@Autowired Filter filter;
	
	public CardsController() {
	}
	
	@Transactional
	@RequestMapping(method = RequestMethod.GET)
	public String getList(
			@RequestParam(value = "id", defaultValue = "0") long id, 
			@RequestParam(value = "url", defaultValue = "") String url,
			Model model) {
		
		List<Card> list = getCardList(id, url, model);
		
		model.addAttribute("cards", list);
		model.addAttribute("filter", filter);
		
		if (!model.containsAttribute("card")) {
			Card card = new Card();
			model.addAttribute("card", card);
		}
		return "cards";
	}

	private List<Card> getCardList(long id, String url, Model model) {
		List<Card> list = null;
		if (url.isEmpty()) {
			filter.setUrl("");
			filter.setId(0);
			list= cards.findAll();
		} else if ("group".equals(url)) {
			Grp grp = groups.findById(id);
			filter.setUrl("group");
			filter.setId(id);
			if (grp != null)
				list=  cards.findGroupCards(grp);
		} else if ("bank".equals(url)) {
			Bank bank = banks.findById(id);
			filter.setUrl("bank");
			filter.setId(id);
			if (bank != null) {
				list = new ArrayList<Card>(bank.getCards());
				for (Card card : list) 
					card.hashCode();
				model.addAttribute("cards", list);
			}	
		} 	
		if (list != null)
			for (Card c: list)
				c.setStat(cards.findStateById(c.getId()));
		return list;
	}
	


	@RequestMapping(value = "/add", method = RequestMethod.GET)
	public String addCard(Model model) {
		Card card = new Card();
		addToModel(model, card);
		return "card-edit";
	}

	@Transactional
	@RequestMapping(value = "/edit", method = RequestMethod.GET)
	public String editCardPage(@RequestParam(value = "id", required = true) long id, Model model) {

		if (!model.containsAttribute("card")) {
			Card card = cards.findById(id);
			addToModel(model, card);

		}
		return "card-edit";
	}

	private void addToModel(Model model, Card card) {
		model.addAttribute("card", card);
		model.addAttribute("places", Place.values());
		model.addAttribute("groups", groups.findAll());
		model.addAttribute("banklst", banks.findAll());
		model.addAttribute("channels", channels.findAll());
		model.addAttribute("limits", cards.getLimits());
	}

	@Transactional
	@RequestMapping(value = "/edit", method = RequestMethod.POST)
	public String editCard(@Valid @ModelAttribute Card card, BindingResult result, Model model,
			RedirectAttributes redirectAttrs, @RequestParam(value = "action", required = true) String action) {

		if (action.equals("cancel")) {
			String message = "Card " + card.toString() + " edit cancelled";
			model.addAttribute("message", message);

		} else if (result.hasErrors()) {
			redirectAttrs.addFlashAttribute("org.springframework.validation.BindingResult.card", result);
			redirectAttrs.addFlashAttribute("card", card);
			redirectAttrs.addFlashAttribute("opers", Oper.values());
			redirectAttrs.addFlashAttribute("places", Place.values());
			redirectAttrs.addFlashAttribute("groups", groups.findAll());
			redirectAttrs.addFlashAttribute("banklst", banks.findAll());
			return "redirect:/cards/edit?id=" + card.getId();

		} else if (action.equals("save") && card != null) {

			cards.makePersistent(card);
			String message = "Card " + card.getId() + " was successfully edited";
			model.addAttribute("message", message);
		}
		if (!filter.getUrl().isEmpty())
			return "redirect:/cards/?url=" + filter.getUrl() + "&id=" + filter.getId();

		return "redirect:/cards";
	}

	@Transactional
	@RequestMapping(value = "/delete", method = RequestMethod.GET)
	public String showSpitterProfile(Model model, @RequestParam(value = "id", required = true) long id,
			@RequestParam(value = "phase", required = true) String phase) {
		Card card = cards.findById(id);
		// System.out.println("card/delete-GET | id = " + id + " | phase = " +
		// phase + " | " + card.toString());

		String view = null;
		if (phase.equals("stage")) {
			model.addAttribute("opers", Oper.values());
			model.addAttribute("places", Place.values());
			model.addAttribute("groups", groups.findAll());
			model.addAttribute("banklst", banks.findAll());
			model.addAttribute("card", card);
			String message = "card " + card.getId() + " queued for display.";
			model.addAttribute("message", message);
			model.addAttribute("action", "delete");
			view = "card-edit";
		}

		if (phase.equals("confirm")) {
			view = "redirect:/cards";
			cards.makeTransient(card);
			String message = "Strategy " + card.getId() + " was successfully deleted";
			model.addAttribute("message", message);
		}

		if (phase.equals("cancel")) {
			view = "redirect:/cards";
			String message = "Strategy delete was cancelled.";
			model.addAttribute("message", message);
		}

		return view;
	}

//	@ExceptionHandler({ DataIntegrityViolationException.class, ConstraintViolationException.class,
//			PersistenceException.class })
//	public ModelAndView dataIntegrityError(HttpServletRequest req, Exception ex) {
//		logger.error("Request: " + req.toString() + " raised " + ex, ex);
//		ModelAndView mav = new ModelAndView();
//		Model model = (Model) mav.getModel();
//		model.addAttribute("exception", ex);
//		model.addAttribute("url", req.getRequestURL());
//		model.addAttribute("url", req.getRequestURL());
//		model.addAttribute("opers", Oper.values());
//		model.addAttribute("places", Place.values());
//		model.addAttribute("groups", groups.findAll());
//		model.addAttribute("banklst", banks.findAll());
//				
//		
//		mav.setViewName("redirect:/cards/edit?id=");
//		
//		
//		return mav;
//	}

	
	@RequestMapping(value = "/stats", method = {RequestMethod.GET, RequestMethod.POST})
	public String stat(
			@RequestParam(value = "id", defaultValue = "0") long id, 
			@RequestParam(value = "url", defaultValue = "") String url, 
			Model model)	{
		List<Card> list = getCardList(id, url, model);
		model.addAttribute("cards", list);

		if (!model.containsAttribute("card")) {
			Card card = new Card();
			model.addAttribute("card", card);
		}
		return "cardsstat";
	}
	
	
	@RequestMapping(value = "/refreshlimits", method = RequestMethod.GET)
	public String refreshLimits(Model model) {
		cards.refreshLimits();
		return "redirect:/cards/stats";
	}
	
	@Transactional
	@RequestMapping(value = "/stat", method = RequestMethod.GET)
	public String statPage(@RequestParam(value = "id", required = true) long id, Model model) {
		Card card = cards.findById(id);
		CardStat st = cards.findStateById(id);
		model.addAttribute("chan", channels.findById(card.getChannelId()));
		model.addAttribute("state", st);
		model.addAttribute("card", card);
		return "card-stat";
	}
}
