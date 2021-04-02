using System;
using System.Collections.Generic;
using System.Text;

namespace InfoPanel.DTOs
{
	public enum EventType
	{
		Arrival, // Boarding
		Departure
	}

	public class AirplaneEvent
	{
		public Guid PlaneId { get; set; }

		public EventType EventType { get; set; }

		public DateTime DepartureTime { get; set; }

		// public int TicketCount { get; set; }
	}
}
